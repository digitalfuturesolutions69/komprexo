package com.komprexo.app.ui

import com.komprexo.app.compression.*
import com.komprexo.app.processing.*

enum class Workflow { BATCH, CONVERT, RESIZE }
enum class ResizeChoice { ORIGINAL, PERCENT, CUSTOM, FIT }
data class EditableSettings(val preset: Preset = Preset.CUSTOM, val targetKiB: String = "200", val customTarget: Boolean = false,
    val format: OutputFormat = OutputFormat.AUTO, val mode: CompressionMode = CompressionMode.QUALITY_FIRST,
    val resizeChoice: ResizeChoice = ResizeChoice.ORIGINAL, val percent: String = "100", val width: String = "", val height: String = "",
    val lockAspect: Boolean = true, val allowAlphaRemoval: Boolean = false) {
    fun resize(): ResizeSpec {
        fun dimension(value: String): Int? = if(value.isBlank()) null else value.toIntOrNull() ?: throw ImageProblem(FailureCode.INVALID_DIMENSIONS)
        return when(resizeChoice) {
            ResizeChoice.ORIGINAL -> ResizeSpec.Original
            ResizeChoice.PERCENT -> ResizeSpec.Percent(percent.toIntOrNull() ?: throw ImageProblem(FailureCode.INVALID_DIMENSIONS))
            ResizeChoice.CUSTOM -> ResizeSpec.Custom(dimension(width),dimension(height),lockAspect)
            ResizeChoice.FIT -> ResizeSpec.Fit(dimension(width) ?: throw ImageProblem(FailureCode.INVALID_DIMENSIONS),dimension(height) ?: throw ImageProblem(FailureCode.INVALID_DIMENSIONS))
        }
    }
    fun options(): PresetOptions {
        val target=targetKiB.toLongOrNull()?.takeIf { it in 1..10240 }?.times(1024) ?: throw ImageProblem(FailureCode.UNREACHABLE_TARGET)
        return PresetOptions(target,format,mode,resize())
    }
    fun apply(preset: Preset): EditableSettings {
        val defaults=preset.defaults()
        val fit=defaults.resize as? ResizeSpec.Fit
        return copy(preset=preset,targetKiB=(defaults.targetBytes/1024).toString(),customTarget=false,format=defaults.format,mode=defaults.mode,
            resizeChoice=if(fit==null) ResizeChoice.ORIGINAL else ResizeChoice.FIT,width=fit?.width?.toString() ?: "",height=fit?.height?.toString() ?: "",lockAspect=true,allowAlphaRemoval=false)
    }
}
