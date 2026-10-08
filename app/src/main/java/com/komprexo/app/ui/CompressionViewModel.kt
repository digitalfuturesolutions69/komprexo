package com.komprexo.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.komprexo.app.compression.*
import com.komprexo.app.storage.ImageStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

data class CompressionUiState(
    val source: ImageSource? = null,
    val originalPreview: Bitmap? = null,
    val outputPreview: Bitmap? = null,
    val result: CompressionResult.Success? = null,
    val busy: Boolean = false,
    val progress: CompressionProgress? = null,
    val error: FailureCode? = null,
    val saved: Boolean = false,
)

class CompressionViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val storage = ImageStorage(application)
    private val engine = AndroidCompressionEngine(storage.cache)
    private val mutable = MutableStateFlow(CompressionUiState(error = if (saved.get<Boolean>("processing") == true) FailureCode.INTERRUPTED else null))
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun select(uri: Uri?) {
        if (uri == null) { notice(FailureCode.CANCELLED); return }
        if (state.value.busy) return
        launch {
            val source = storage.import(uri)
            var accepted = false
            try {
                val preview = withContext(Dispatchers.Default) { BitmapCodec.decode(source, preview = true) }
                ensureActive()
                storage.deleteResult(state.value.result)
                state.value.source?.file?.delete()
                mutable.value = CompressionUiState(source = source, originalPreview = preview, busy = true)
                accepted = true
            } finally { if (!accepted) source.file.delete() }
        }
    }

    fun compress(maxBytes: Long, format: OutputFormat, mode: CompressionMode = CompressionMode.QUALITY_FIRST) {
        val source = state.value.source ?: return
        if (state.value.busy) return
        launch {
            val result = engine.compress(CompressionRequest(source, maxBytes, CompressionOptions(format, mode))) { progress ->
                mutable.update { it.copy(progress = progress) }
            }
            when (result) {
                is CompressionResult.Failed -> notice(result.failure.code)
                is CompressionResult.Success -> {
                    var accepted = false
                    try {
                        val preview = withContext(Dispatchers.Default) { BitmapCodec.decode(BitmapCodec.inspect(result.file), preview = true) }
                        ensureActive()
                        storage.deleteResult(state.value.result)
                        mutable.update { it.copy(result = result, outputPreview = preview) }
                        accepted = true
                    } finally { if (!accepted) storage.deleteResult(result) }
                }
            }
        }
    }

    fun save(uri: Uri?) {
        if (uri == null) { notice(FailureCode.CANCELLED); return }
        val result = state.value.result ?: return
        if (state.value.busy) return
        launch { storage.save(result, uri); mutable.update { it.copy(saved = true) } }
    }
    fun share(onReady: (android.content.Intent) -> Unit) {
        val result = state.value.result ?: return
        if (state.value.busy) return
        launch { onReady(storage.shareIntent(result)) }
    }
    fun cancel() { job?.cancel() }
    fun notice(code: FailureCode) { mutable.update { it.copy(error = code) } }
    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        mutable.update { it.copy(busy = true, error = null, saved = false, progress = null) }
        saved["processing"] = true
        job = viewModelScope.launch {
            try { block() }
            catch (_: CancellationException) { notice(FailureCode.CANCELLED) }
            catch (e: ImageProblem) { notice(e.code) }
            catch (_: OutOfMemoryError) { notice(FailureCode.INSUFFICIENT_MEMORY) }
            catch (_: SecurityException) { notice(FailureCode.FILE_ACCESS) }
            catch (e: IOException) { notice(storageFailure(e)) }
            catch (_: RuntimeException) { notice(FailureCode.INTERRUPTED) }
            finally { saved["processing"] = false; mutable.update { it.copy(busy = false) } }
        }
    }
    override fun onCleared() {
        job?.cancel()
        storage.deleteResult(state.value.result)
        state.value.source?.file?.delete()
        super.onCleared()
    }
}
