package com.komprexo.app.processing

import com.komprexo.app.compression.*

enum class Preset { DOCUMENT, MARKETPLACE, WEBSITE, SOCIAL, CUSTOM }
data class PresetOptions(val targetBytes: Long, val format: OutputFormat, val mode: CompressionMode, val resize: ResizeSpec)
fun Preset.defaults() = when (this) {
    // Auto favors JPEG for opaque photos, preserves alpha for other inputs.
    Preset.DOCUMENT -> PresetOptions(200L*1024,OutputFormat.AUTO,CompressionMode.QUALITY_FIRST,ResizeSpec.Original)
    Preset.MARKETPLACE -> PresetOptions(500L*1024,OutputFormat.AUTO,CompressionMode.QUALITY_FIRST,ResizeSpec.Fit(1600,1600))
    Preset.WEBSITE -> PresetOptions(500L*1024,OutputFormat.WEBP,CompressionMode.QUALITY_FIRST,ResizeSpec.Fit(1280,ImageLimits.MAX_DIMENSION))
    Preset.SOCIAL -> PresetOptions(1024L*1024,OutputFormat.AUTO,CompressionMode.QUALITY_FIRST,ResizeSpec.Fit(1080,1080))
    Preset.CUSTOM -> PresetOptions(200L*1024,OutputFormat.AUTO,CompressionMode.QUALITY_FIRST,ResizeSpec.Original)
}
