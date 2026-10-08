package com.komprexo.app.compression

import java.io.File

enum class OutputFormat(val mime: String, val extension: String) {
    AUTO("", ""), JPEG("image/jpeg", "jpg"), PNG("image/png", "png"), WEBP("image/webp", "webp")
}

data class CompressionOptions(val format: OutputFormat = OutputFormat.AUTO)
data class CompressionRequest(val source: ImageSource, val maxBytes: Long, val options: CompressionOptions = CompressionOptions())
data class ImageSource(val file: File, val bytes: Long, val width: Int, val height: Int, val mime: String, val orientation: Int)
data class CompressionProgress(val attempts: Int, val width: Int, val height: Int)
enum class FailureCode { INVALID_IMAGE, CORRUPT_IMAGE, UNSUPPORTED_FORMAT, INPUT_TOO_LARGE, INSUFFICIENT_MEMORY, UNREACHABLE_TARGET, FILE_ACCESS, STORAGE_FULL, CANCELLED, INTERRUPTED }
data class CompressionFailure(val code: FailureCode)
sealed interface CompressionResult {
    data class Success(val file: File, val originalBytes: Long, val bytes: Long, val width: Int, val height: Int,
                       val format: OutputFormat, val quality: Int, val maxBytes: Long, val transparencyRemoved: Boolean) : CompressionResult {
        val meetsTarget: Boolean get() = bytes <= maxBytes && bytes > 0
        val reductionPercent: Double get() = 100.0 * (1.0 - bytes.toDouble() / originalBytes)
    }
    data class Failed(val failure: CompressionFailure) : CompressionResult
}
class ImageProblem(val code: FailureCode) : Exception()
interface CompressionEngine {
    suspend fun compress(request: CompressionRequest, progress: (CompressionProgress) -> Unit = {}): CompressionResult
}
object ImageLimits {
    const val MAX_INPUT_BYTES = 32L * 1024 * 1024
    const val MAX_SOURCE_PIXELS = 128_000_000L
    const val MAX_DIMENSION = 32_768
    const val MAX_DECODE_PIXELS = 2_000_000L
    const val MAX_DECODE_EDGE = 2048
    const val MAX_TARGET_BYTES = 10L * 1024 * 1024
    const val MIN_TARGET_BYTES = 1L
    const val MIN_QUALITY = 35
}
