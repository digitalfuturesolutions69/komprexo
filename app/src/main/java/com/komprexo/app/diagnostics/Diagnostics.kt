package com.komprexo.app.diagnostics

import android.os.Build
import android.util.Log
import com.komprexo.app.BuildConfig
import com.komprexo.app.compression.*
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException

enum class ProcessingStage { SELECT, MIME_QUERY, URI_OPEN, URI_READ, STRUCTURE, BOUNDS, METADATA, MEMORY_PLAN, DECODE, NORMALIZE, SCALE, ENCODE, OUTPUT_VALIDATE, READY }
enum class CodecName { NONE, BITMAP_FACTORY, JPEG, PNG, WEBP_LEGACY, WEBP_LOSSY }
enum class AllocationFailure { NONE, HEAP_BUDGET, PIXEL_BOUND, OUT_OF_MEMORY }
enum class ExceptionCategory { NONE, PERMISSION, INVALID_URI, READ, STRUCTURE, UNSUPPORTED, DECODE, ENCODE, OUTPUT, MEMORY, IO, CANCELLED, ARGUMENT, STATE, UNEXPECTED }

data class DiagnosticEvent(val api: Int, val manufacturer: String, val model: String, val stage: ProcessingStage,
    val declaredMime: String, val decoder: CodecName, val encoder: CodecName,
    val exception: ExceptionCategory, val allocation: AllocationFailure) {
    fun json(): String = JSONObject().apply {
        put("api", api); put("manufacturer", manufacturer); put("model", model)
        put("stage", stage.name); put("declaredMime", declaredMime)
        put("decoder", decoder.name); put("encoder", encoder.name)
        put("exception", exception.name); put("allocation", allocation.name)
    }.toString()
}

/** Operation-local state: never stores URI, filename, bytes, EXIF, or exception text. */
class DiagnosticTrace(private val emit: (DiagnosticEvent) -> Unit = {
    if (BuildConfig.DEBUG) Log.d("KomprexoDiagnostics", it.json())
}) {
    var stage = ProcessingStage.SELECT
    var declaredMime = "unknown"
        private set
    var decoder = CodecName.NONE
    var encoder = CodecName.NONE
    fun mime(value: String?) {
        declaredMime = when (value?.lowercase(java.util.Locale.ROOT)) {
            "image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/heic-sequence", "image/heif-sequence", "application/octet-stream" -> value.lowercase(java.util.Locale.ROOT)
            null -> "unknown"
            else -> "other"
        }
    }
    fun move(value: ProcessingStage) { stage = value; record() }
    fun record(error: Throwable? = null) {
        val allocation = if (error is OutOfMemoryError) AllocationFailure.OUT_OF_MEMORY else (error as? ImageProblem)?.allocation ?: AllocationFailure.NONE
        val category = when (error) {
            null -> ExceptionCategory.NONE
            is CancellationException -> ExceptionCategory.CANCELLED
            is SecurityException -> ExceptionCategory.PERMISSION
            is OutOfMemoryError -> ExceptionCategory.MEMORY
            is IOException -> ExceptionCategory.IO
            is ImageProblem -> when (error.code) {
                FailureCode.READ_PERMISSION -> ExceptionCategory.PERMISSION
                FailureCode.INVALID_URI -> ExceptionCategory.INVALID_URI
                FailureCode.READ_FAILED -> ExceptionCategory.READ
                FailureCode.CORRUPT_IMAGE, FailureCode.INVALID_IMAGE -> ExceptionCategory.STRUCTURE
                FailureCode.UNSUPPORTED_FORMAT, FailureCode.UNSUPPORTED_HEIF -> ExceptionCategory.UNSUPPORTED
                FailureCode.DECODER_FAILED -> ExceptionCategory.DECODE
                FailureCode.ENCODER_FAILED -> ExceptionCategory.ENCODE
                FailureCode.OUTPUT_INVALID -> ExceptionCategory.OUTPUT
                FailureCode.INSUFFICIENT_MEMORY -> ExceptionCategory.MEMORY
                else -> ExceptionCategory.IO
            }
            is IllegalArgumentException -> ExceptionCategory.ARGUMENT
            is IllegalStateException -> ExceptionCategory.STATE
            else -> ExceptionCategory.UNEXPECTED
        }
        emit(DiagnosticEvent(Build.VERSION.SDK_INT, deviceLabel(Build.MANUFACTURER), deviceLabel(Build.MODEL), stage, declaredMime, decoder, encoder, category, allocation))
    }
    companion object {
        private fun deviceLabel(value: String): String = value.filter { it.isLetterOrDigit() || it in " ._-" }.take(48)
    }
}

fun classifyFailure(error: Throwable, stage: ProcessingStage): FailureCode = when (error) {
    is ImageProblem -> error.code
    is CancellationException -> FailureCode.CANCELLED
    is OutOfMemoryError -> FailureCode.INSUFFICIENT_MEMORY
    is SecurityException -> if (stage in setOf(ProcessingStage.MIME_QUERY, ProcessingStage.URI_OPEN, ProcessingStage.URI_READ)) FailureCode.READ_PERMISSION else FailureCode.FILE_ACCESS
    is IOException -> if (storageFailure(error) == FailureCode.STORAGE_FULL) FailureCode.STORAGE_FULL
        else if (stage in setOf(ProcessingStage.URI_OPEN, ProcessingStage.URI_READ)) FailureCode.READ_FAILED else FailureCode.FILE_ACCESS
    else -> when (stage) {
        ProcessingStage.BOUNDS, ProcessingStage.DECODE, ProcessingStage.NORMALIZE, ProcessingStage.METADATA -> FailureCode.DECODER_FAILED
        ProcessingStage.ENCODE -> FailureCode.ENCODER_FAILED
        ProcessingStage.OUTPUT_VALIDATE -> FailureCode.OUTPUT_INVALID
        ProcessingStage.URI_OPEN, ProcessingStage.URI_READ -> FailureCode.READ_FAILED
        else -> FailureCode.INTERRUPTED
    }
}
