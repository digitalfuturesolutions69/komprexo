package com.komprexo.app.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.komprexo.app.compression.*
import com.komprexo.app.access.*
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
    val exports: List<ExportItem> = emptyList(), val busy: Boolean = false, val error: FailureCode? = null,
    val multiple: Boolean = false, val restriction: Restriction? = null)
val Phase2State.isBatch get() = workflow == Workflow.BATCH || multiple

class Phase2ViewModel @JvmOverloads constructor(application: Application, private val saved: SavedStateHandle, val quota: DailyQuotaManager = AccessServices.quota(application)) : AndroidViewModel(application) {
    private val storage=ImageStorage(application)
    private val engine=AndroidCompressionEngine(storage.cache)
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
    fun edit(settings: EditableSettings) {
        if(state.value.busy) return
        if(!FeatureAccessPolicy.allowsPreset(quota.entitlement.value,settings.preset)) {
            mutable.update { it.copy(restriction=Restriction.PREMIUM_PRESET) };return
        }
        mutable.update { it.copy(settings=settings) }
    }
    fun setMultiple(multiple: Boolean) {
        if(state.value.busy || state.value.workflow==Workflow.BATCH) return
        // Switching off batch cannot discard a multi-image selection.
        if(!multiple && state.value.selection.size>1) { notice(FailureCode.BATCH_LIMIT);return }
        mutable.update { it.copy(multiple=multiple) }
    }
    fun clearRestriction() { mutable.update { it.copy(restriction=null) } }
    fun select(uris: List<Uri>) {
        if(state.value.busy) { notice(FailureCode.BUSY);return }
        if(uris.isEmpty()) return
        val isBatch=state.value.isBatch
        val count=uris.size + if(isBatch) state.value.selection.size else 0
        if(count>if(isBatch) MAX_BATCH_IMAGES else 1) { notice(FailureCode.BATCH_LIMIT); return }
        if(isBatch && count>FeatureAccessPolicy.batchLimit(quota.entitlement.value)) {
            mutable.update { it.copy(restriction=Restriction.BATCH_LIMIT) };return
        }
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
        launch {
            val operation=when(snapshot.workflow) { Workflow.BATCH->Operation.COMPRESS; Workflow.RESIZE->Operation.RESIZE; Workflow.CONVERT->Operation.CONVERT }
            val reservation=quota.reserve(operation,snapshot.selection.size,if(snapshot.workflow==Workflow.CONVERT) Preset.CUSTOM else snapshot.settings.preset)
            // Restrictions preserve existing results, selection and settings.
            clearOutputs()
            var items=snapshot.selection.map { BatchItem(BatchInput(it.id,it.source,it.error)) }
            fun publish(current: Int? = null) {
                mutable.update { it.copy(progress=if(snapshot.isBatch) BatchProgress(items,current) else null,
                    outputs=items.mapNotNull { item -> (item.result as? ItemResult.Success)?.output }) }
            }
            try {
                publish()
                val options=if(operation==Operation.COMPRESS) snapshot.settings.options() else null
                val resize=if(operation==Operation.RESIZE) snapshot.settings.resize() else ResizeSpec.Original
                for(index in items.indices) {
                    ensureActive();publish(index+1)
                    val input=items[index].input
                    val result=if(input.source==null) ProcessingResult.Failed(input.importError ?: FailureCode.INVALID_IMAGE)
                    else if(options!=null) {
                        when(val compressed=engine.compress(CompressionRequest(input.source,options.targetBytes,
                            CompressionOptions(options.format,options.mode,options.resize)))) {
                            is CompressionResult.Success->ProcessingResult.Success(compressed.output())
                            is CompressionResult.Failed->ProcessingResult.Failed(compressed.failure.code)
                        }
                    } else transform.transform(TransformRequest(input.source,snapshot.settings.format,resize,snapshot.settings.allowAlphaRemoval))
                    when(result) {
                        is ProcessingResult.Failed -> {
                            items=items.toMutableList().also { it[index]=BatchItem(input,ItemResult.Failed(result.code)) }
                            if(!snapshot.isBatch) notice(result.code)
                            publish()
                        }
                        is ProcessingResult.Success -> {
                            var accepted=false
                            try {
                                ensureActive()
                                withContext(NonCancellable) {
                                    check(quota.settle(reservation,index))
                                    items=items.toMutableList().also { it[index]=BatchItem(input,ItemResult.Success(result.output)) }
                                    publish();accepted=true
                                }
                            } finally { if(!accepted) result.output.file.parentFile?.deleteRecursively() }
                        }
                    }
                }
            } catch(e: CancellationException) {
                items=items.map { if(it.result==ItemResult.Pending) it.copy(result=ItemResult.Cancelled) else it }
                publish();throw e
            } finally { quota.release(reservation) }
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
            catch(e: AccessDenied) { mutable.update { it.copy(restriction=e.reason) } }
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
