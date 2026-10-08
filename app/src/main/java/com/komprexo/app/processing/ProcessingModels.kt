package com.komprexo.app.processing

import com.komprexo.app.compression.*
import java.io.File
import kotlin.math.floor

const val MAX_BATCH_IMAGES = 20
const val MAX_SELECTION_BYTES = 256L * 1024 * 1024
const val MAX_TRANSFORM_BYTES = ImageLimits.MAX_INPUT_BYTES

data class Dimensions(val width: Int, val height: Int)
fun ImageSource.visualDimensions() = if (orientation in 5..8) Dimensions(height, width) else Dimensions(width, height)

sealed interface ResizeSpec {
    data object Original : ResizeSpec
    data class Percent(val value: Int) : ResizeSpec
    data class Custom(val width: Int?, val height: Int?, val lockAspect: Boolean = true) : ResizeSpec
    /** Bounding box, never crop, distort or upscale. */
    data class Fit(val width: Int, val height: Int) : ResizeSpec
}
fun ResizeSpec.resolve(source: Dimensions): Dimensions {
    fun checked(n: Int?) { if (n != null && n !in 1..ImageLimits.MAX_DIMENSION) throw ImageProblem(FailureCode.INVALID_DIMENSIONS) }
    val result = when (this) {
        ResizeSpec.Original -> source
        is ResizeSpec.Percent -> {
            if (value !in 1..100) throw ImageProblem(FailureCode.INVALID_DIMENSIONS)
            Dimensions(maxOf(1,source.width * value / 100), maxOf(1,source.height * value / 100))
        }
        is ResizeSpec.Fit -> {
            checked(width); checked(height)
            val scale = minOf(1.0,width.toDouble()/source.width,height.toDouble()/source.height)
            Dimensions(maxOf(1,floor(source.width*scale).toInt()),maxOf(1,floor(source.height*scale).toInt()))
        }
        is ResizeSpec.Custom -> {
            checked(width); checked(height)
            if (width == null && height == null) throw ImageProblem(FailureCode.INVALID_DIMENSIONS)
            if (lockAspect || width == null || height == null) {
                val scale = minOf(width?.toDouble()?.div(source.width) ?: Double.POSITIVE_INFINITY,
                    height?.toDouble()?.div(source.height) ?: Double.POSITIVE_INFINITY)
                Dimensions(maxOf(1,floor(source.width*scale).toInt()),maxOf(1,floor(source.height*scale).toInt()))
            } else Dimensions(width,height)
        }
    }
    if (result.width > source.width || result.height > source.height) throw ImageProblem(FailureCode.INVALID_DIMENSIONS)
    if (result.width !in 1..ImageLimits.MAX_DIMENSION || result.height !in 1..ImageLimits.MAX_DIMENSION) throw ImageProblem(FailureCode.INVALID_DIMENSIONS)
    return result
}

data class ImageOutput(val file: File, val originalBytes: Long, val bytes: Long, val width: Int, val height: Int,
    val format: OutputFormat, val transparencyRemoved: Boolean = false, val targetBytes: Long? = null) {
    val meetsTarget: Boolean? get() = targetBytes?.let { bytes in 1..it }
    val reductionPercent: Double get() = 100.0 * (1.0 - bytes.toDouble()/originalBytes)
}
fun CompressionResult.Success.output() = ImageOutput(file,originalBytes,bytes,width,height,format,transparencyRemoved,maxBytes)
sealed interface ProcessingResult {
    data class Success(val output: ImageOutput) : ProcessingResult
    data class Failed(val code: FailureCode) : ProcessingResult
}
data class TransformRequest(val source: ImageSource, val format: OutputFormat, val resize: ResizeSpec = ResizeSpec.Original,
    val allowAlphaRemoval: Boolean = false)

interface TransformEngine { suspend fun transform(request: TransformRequest): ProcessingResult }
data class BatchInput(val id: String, val source: ImageSource?, val importError: FailureCode? = null)
sealed interface ItemResult {
    data object Pending : ItemResult
    data object Cancelled : ItemResult
    data class Failed(val code: FailureCode) : ItemResult
    data class Success(val output: ImageOutput) : ItemResult
}
data class BatchItem(val input: BatchInput, val result: ItemResult = ItemResult.Pending)
data class BatchProgress(val items: List<BatchItem>, val current: Int? = null) {
    val total get() = items.size
    val completed get() = items.count { it.result is ItemResult.Success }
    val failed get() = items.count { it.result is ItemResult.Failed }
    val percent get() = if (total == 0) 0 else (completed + failed) * 100 / total
}
