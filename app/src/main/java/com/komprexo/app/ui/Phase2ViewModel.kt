package com.komprexo.app.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import com.komprexo.app.storage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

// Thumbnails are at most160x160 pixels, never full decoded sources.
data class Selection(val id: String, val source: ImageSource?, val thumbnail: Bitmap? = null, val error: FailureCode? = null)
data class Phase2State(val workflow: Workflow = Workflow.BATCH, val settings: EditableSettings = EditableSettings(),
    val selection: List<Selection> = emptyList(), val progress: BatchProgress? = null, val outputs: List<ImageOutput> = emptyList(),
    val exports: List<ExportItem> = emptyList(), val busy: Boolean = false, val error: FailureCode? = null)

class Phase2ViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val storage=ImageStorage(application)
    private val batch=BatchOrchestrator(AndroidCompressionEngine(storage.cache))
    private val transform=ImageTransformEngine(storage.cache)
    private val mutable=MutableStateFlow(Phase2State(error=if(saved.get<Boolean>("phase2busy")==true) FailureCode.INTERRUPTED else null))
    val state=mutable.asStateFlow()
    private var job: Job? = null
    fun enter(workflow: Workflow) {
        if(state.value.busy) return
        if(state.value.workflow!=workflow) {
            clear()
            mutable.value=Phase2State(workflow=workflow,settings=EditableSettings(format=if(workflow==Workflow.CONVERT) OutputFormat.PNG else OutputFormat.AUTO))
        }
    }
    fun edit(settings: EditableSettings) { if(!state.value.busy) mutable.update { it.copy(settings=settings) } }
    fun select(uris: List<Uri>) {
        if(state.value.busy) { notice(FailureCode.BUSY);return }
        if(uris.isEmpty()) return
        val isBatch=state.value.workflow==Workflow.BATCH
        val count=uris.size + if(isBatch) state.value.selection.size else 0
        if(count>if(isBatch) MAX_BATCH_IMAGES else 1) { notice(FailureCode.BATCH_LIMIT); return }
        mutable.update { it.copy(settings=it.settings.copy(allowAlphaRemoval=false)) }
        if(!isBatch) clear()
        clearOutputs()
        launch {
            for(uri in uris) {
                ensureActive()
                val id=UUID.randomUUID().toString()
                var source: ImageSource?=null
                var accepted=false
                try {
                val selection=try {
                    val imported=storage.import(uri,validatePreview=false)
                    source=imported
                    if((state.value.selection.sumOf { it.source?.bytes ?: 0 } + imported.bytes)>MAX_SELECTION_BYTES) throw ImageProblem(FailureCode.STORAGE_FULL)
                    val thumbnail=withContext(Dispatchers.Default) { AndroidCompressionEngine.memoryGate.withLock {
                        val decoded=BitmapCodec.decode(imported,preview=true)
                        try {
                            val factor=minOf(1.0,160.0/decoded.width,160.0/decoded.height)
                            val thumb=Bitmap.createScaledBitmap(decoded,maxOf(1,(decoded.width*factor).toInt()),maxOf(1,(decoded.height*factor).toInt()),true)
                            if(thumb!==decoded) decoded.recycle()
                            thumb
                        } catch(t: Throwable) { decoded.recycle();throw t }
                    } }
                    ensureActive();Selection(id,imported,thumbnail)
                } catch(e: CancellationException) { throw e }
                  catch(e: ImageProblem) { Selection(id,null,error=e.code) }
                  catch(_: OutOfMemoryError) { Selection(id,null,error=FailureCode.INSUFFICIENT_MEMORY) }
                  catch(_: Exception) { Selection(id,null,error=FailureCode.READ_FAILED) }
                if(selection.source==null) source?.file?.delete()
                ensureActive();mutable.update { it.copy(selection=it.selection+selection) };accepted=true
                } finally { if(!accepted) source?.file?.delete() }
            }
        }
    }
    fun remove(id: String) {
        if(state.value.busy) return
        state.value.selection.find { it.id==id }?.source?.file?.delete()
        clearOutputs();mutable.update { it.copy(selection=it.selection.filterNot { item -> item.id==id }) }
    }
    fun clear() {
        if(state.value.busy) return
        state.value.selection.forEach { it.source?.file?.delete() }
        clearOutputs();mutable.update { it.copy(selection=emptyList(),error=null,exports=emptyList()) }
    }
    private fun clearOutputs() {
        state.value.outputs.forEach { it.file.parentFile?.deleteRecursively() }
        mutable.update { it.copy(outputs=emptyList(),progress=null,exports=emptyList()) }
    }
    fun start() {
        if(state.value.busy || state.value.selection.isEmpty()) return
        val snapshot=state.value
        if(snapshot.settings.format==OutputFormat.JPEG && snapshot.selection.any { it.thumbnail?.hasAlpha()==true } && !snapshot.settings.allowAlphaRemoval) {
            notice(FailureCode.ALPHA_CONFIRMATION);return
        }
        clearOutputs()
        launch {
            if(snapshot.workflow==Workflow.BATCH) {
                batch.process(snapshot.selection.map { BatchInput(it.id,it.source,it.error) },snapshot.settings.options()) { progress ->
                    mutable.update { it.copy(progress=progress,outputs=progress.items.mapNotNull { item -> (item.result as? ItemResult.Success)?.output }) }
                }
            } else {
                val input=snapshot.selection.single()
                val source=input.source ?: throw ImageProblem(input.error ?: FailureCode.INVALID_IMAGE)
                when(val result=transform.transform(TransformRequest(source,snapshot.settings.format,
                    if(snapshot.workflow==Workflow.CONVERT) ResizeSpec.Original else snapshot.settings.resize(),snapshot.settings.allowAlphaRemoval))) {
                    is ProcessingResult.Failed -> notice(result.code)
                    is ProcessingResult.Success -> {
                        var accepted=false
                        try { ensureActive();mutable.update { it.copy(outputs=listOf(result.output)) };accepted=true }
                        finally { if(!accepted) result.output.file.parentFile?.deleteRecursively() }
                    }
                }
            }
        }
    }
    fun save(output: ImageOutput, destination: Uri?) {
        if(destination==null || state.value.busy) return
        launch { storage.saveOutput(output,destination);mutable.update { it.copy(exports=it.exports+ExportItem(output,destination)) } }
    }
    fun saveFolder(tree: Uri?) {
        if(tree==null || state.value.outputs.isEmpty() || state.value.busy) return
        val outputs=state.value.outputs
        launch { storage.saveToFolder(outputs,tree) { items -> mutable.update { it.copy(exports=items) } } }
    }
    fun share(outputs: List<ImageOutput>, ready: (Intent)->Unit) {
        if(outputs.isEmpty() || state.value.busy) return
        launch { ready(storage.shareOutputs(outputs)) }
    }
    fun cancel() { job?.cancel() }
    fun notice(code: FailureCode) { mutable.update { it.copy(error=code) } }
    private fun launch(block: suspend CoroutineScope.()->Unit) {
        if(state.value.busy) return
        mutable.update { it.copy(busy=true,error=null) };saved["phase2busy"]=true
        job=viewModelScope.launch {
            try { block() }
            catch(_: CancellationException) { notice(FailureCode.CANCELLED) }
            catch(e: ImageProblem) { notice(e.code) }
            catch(_: OutOfMemoryError) { notice(FailureCode.INSUFFICIENT_MEMORY) }
            catch(_: SecurityException) { notice(FailureCode.FILE_ACCESS) }
            catch(e: IOException) { notice(storageFailure(e)) }
            catch(_: Exception) { notice(FailureCode.INTERRUPTED) }
            finally { saved["phase2busy"]=false;mutable.update { it.copy(busy=false) } }
        }
    }
    override fun onCleared() {
        job?.cancel()
        state.value.selection.forEach { it.source?.file?.delete() }
        state.value.outputs.forEach { it.file.parentFile?.deleteRecursively() }
        super.onCleared()
    }
}
