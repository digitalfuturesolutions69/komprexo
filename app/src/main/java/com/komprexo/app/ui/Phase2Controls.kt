package com.komprexo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komprexo.app.R
import com.komprexo.app.access.*
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*

fun presetLabel(preset: Preset): Int = when(preset) {
    Preset.DOCUMENT -> R.string.preset_document
    Preset.MARKETPLACE -> R.string.preset_marketplace
    Preset.WEBSITE -> R.string.preset_website
    Preset.SOCIAL -> R.string.preset_social
    Preset.CUSTOM -> R.string.custom
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetChoices(settings: EditableSettings, enabled: Boolean, choices: List<Preset> = Preset.entries, change: (EditableSettings)->Unit) {
    Text(stringResource(R.string.smart_presets),style=MaterialTheme.typography.titleMedium,modifier=Modifier.fillMaxWidth())
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        choices.forEach { preset -> ChoiceChip(settings.preset==preset,{ change(settings.apply(preset)) },enabled=enabled,
            label={ Text(if(preset in FeatureAccessPolicy.premiumPresets) stringResource(R.string.preset_premium_label,stringResource(presetLabel(preset))) else stringResource(presetLabel(preset))) },modifier=Modifier.heightIn(min=48.dp)) }
    }
    Text(stringResource(R.string.preset_editable),style=MaterialTheme.typography.bodySmall)
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetControls(settings: EditableSettings, enabled: Boolean, change: (EditableSettings)->Unit) {
    Text(stringResource(R.string.maximum_size),style=MaterialTheme.typography.titleMedium,modifier=Modifier.fillMaxWidth())
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(100,200,500,1024,2048).forEach { size -> ChoiceChip(!settings.customTarget && settings.targetKiB==size.toString(),
            { change(settings.copy(targetKiB=size.toString(),customTarget=false)) },enabled=enabled,label={ Text(if(size>=1024) "${size/1024} MB" else "$size KB") },modifier=Modifier.heightIn(min=48.dp)) }
        ChoiceChip(settings.customTarget,{ change(settings.copy(customTarget=true)) },enabled=enabled,label={ Text(stringResource(R.string.custom)) },modifier=Modifier.heightIn(min=48.dp).testTag("phase2CustomTarget"))
    }
    if(settings.customTarget) NumberField(settings.targetKiB,{ change(settings.copy(targetKiB=it)) },R.string.custom_label,enabled,"phase2Target")
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatControls(settings: EditableSettings, enabled: Boolean, allowAuto: Boolean, alpha: Boolean, change: (EditableSettings)->Unit) {
    Text(stringResource(R.string.output_format),style=MaterialTheme.typography.titleMedium,modifier=Modifier.fillMaxWidth())
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutputFormat.entries.filter { allowAuto || it!=OutputFormat.AUTO }.forEach { format -> ChoiceChip(settings.format==format,
            { change(settings.copy(format=format,allowAlphaRemoval=false)) },enabled=enabled,label={ Text(if(format==OutputFormat.AUTO) stringResource(R.string.auto_recommended) else format.name) },modifier=Modifier.heightIn(min=48.dp)) }
    }
    if(settings.format==OutputFormat.JPEG) {
        Text(stringResource(R.string.jpeg_alpha))
        if(alpha) AlphaConsent(settings,enabled,change)
    }
}
@Composable
fun AlphaConsent(settings: EditableSettings, enabled: Boolean, change: (EditableSettings)->Unit) {
    val description=stringResource(R.string.alpha_confirm)
    Text(stringResource(R.string.jpeg_alpha))
    Row {
        Checkbox(settings.allowAlphaRemoval,{ change(settings.copy(allowAlphaRemoval=it)) },enabled=enabled,
            modifier=Modifier.heightIn(min=48.dp).testTag("alphaConfirmation").semantics { contentDescription=description })
        Text(description,modifier=Modifier.padding(top=12.dp))
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResizeControls(settings: EditableSettings, enabled: Boolean, change: (EditableSettings)->Unit) {
    Text(stringResource(R.string.resize_dimensions),style=MaterialTheme.typography.titleMedium,modifier=Modifier.fillMaxWidth())
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ChoiceChip(settings.resizeChoice==ResizeChoice.ORIGINAL,{ change(settings.copy(resizeChoice=ResizeChoice.ORIGINAL)) },enabled=enabled,
            label={ Text(stringResource(R.string.keep_dimensions)) },modifier=Modifier.heightIn(min=48.dp))
        listOf(25,50,75,100).forEach { percent -> ChoiceChip(settings.resizeChoice==ResizeChoice.PERCENT && settings.percent==percent.toString(),
            { change(settings.copy(resizeChoice=ResizeChoice.PERCENT,percent=percent.toString())) },enabled=enabled,label={ Text("$percent%") },modifier=Modifier.heightIn(min=48.dp)) }
        ChoiceChip(settings.resizeChoice==ResizeChoice.CUSTOM,{ change(settings.copy(resizeChoice=ResizeChoice.CUSTOM)) },enabled=enabled,
            label={ Text(stringResource(R.string.custom_dimensions)) },modifier=Modifier.heightIn(min=48.dp).testTag("customDimensions"))
    }
    if(settings.resizeChoice==ResizeChoice.PERCENT) NumberField(settings.percent,{ change(settings.copy(percent=it)) },R.string.scale_percent,enabled,"resizePercent")
    if(settings.resizeChoice in listOf(ResizeChoice.CUSTOM,ResizeChoice.FIT)) {
        NumberField(settings.width,{ change(settings.copy(width=it)) },R.string.width_pixels,enabled,"resizeWidth")
        NumberField(settings.height,{ change(settings.copy(height=it)) },R.string.height_pixels,enabled,"resizeHeight")
        if(settings.resizeChoice==ResizeChoice.CUSTOM) {
            val description=stringResource(R.string.aspect_lock)
            Row {
            Checkbox(settings.lockAspect,{ change(settings.copy(lockAspect=it)) },enabled=enabled,modifier=Modifier.heightIn(min=48.dp).testTag("aspectLock").semantics { contentDescription=description })
            Text(stringResource(R.string.aspect_lock),modifier=Modifier.padding(top=12.dp))
        }
        }
        Text(stringResource(if(settings.resizeChoice==ResizeChoice.FIT) R.string.fit_no_crop else R.string.resize_no_upscale),style=MaterialTheme.typography.bodySmall)
    }
    if(settings.preset==Preset.MARKETPLACE) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(1200,1600,2000).forEach { edge -> ChoiceChip(settings.resizeChoice==ResizeChoice.FIT && settings.width==edge.toString() && settings.height==edge.toString(), { change(settings.copy(resizeChoice=ResizeChoice.FIT,width=edge.toString(),height=edge.toString())) },
            label={ Text("$edge × $edge") },enabled=enabled,modifier=Modifier.heightIn(min=48.dp)) }
    }
    if(settings.preset==Preset.SOCIAL) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(1080 to 1080,1080 to 1350,1920 to 1080).forEach { (width,height) ->
            ChoiceChip(settings.resizeChoice==ResizeChoice.FIT && settings.width==width.toString() && settings.height==height.toString(), { change(settings.copy(resizeChoice=ResizeChoice.FIT,width=width.toString(),height=height.toString())) },
                label={ Text((if(width==height) "1:1" else if(height==1350) "4:5" else "16:9")+" · $width × $height") },enabled=enabled,modifier=Modifier.heightIn(min=48.dp))
        }
    }
}
@Composable
private fun NumberField(value: String, change: (String)->Unit, label: Int, enabled: Boolean, tag: String) {
    OutlinedTextField(value,{ change(it.take(5)) },label={ Text(stringResource(label)) },enabled=enabled,singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth().testTag(tag))
}
