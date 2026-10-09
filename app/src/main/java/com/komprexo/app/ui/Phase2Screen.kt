package com.komprexo.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komprexo.app.R
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Phase2Screen(model: Phase2ViewModel, onHome: ()->Unit, onUpgrade: ()->Unit = {}) {
    val state by model.state.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val batch=state.isBatch
    val compression=state.workflow==Workflow.BATCH
    fun resultOf(selection: Selection) = state.progress?.items?.find { it.input.id==selection.id }?.result
    fun group(selection: Selection) = when(resultOf(selection)) { is ItemResult.Success -> 0; is ItemResult.Failed -> 1; ItemResult.Cancelled -> 2; else -> 3 }
    val grouped=batch && state.progress?.let { it.current==null && it.items.none { item -> item.result==ItemResult.Pending } }==true
    val visible=state.selection.withIndex().toList().let { if(grouped) it.sortedBy { item -> group(item.value) } else it }

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pendingSave by rememberSaveable { mutableStateOf<String?>(null) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_BATCH_IMAGES)) { model.select(it) }
    val single=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { uri -> model.select(listOf(uri)) } }
    val save=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        val output=state.outputs.find { it.file.parentFile?.name==pendingSave }
        if(output!=null && response.resultCode==Activity.RESULT_OK) model.save(output,response.data?.data)
        pendingSave=null
    }
    val folder=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        if(response.resultCode==Activity.RESULT_OK) model.saveFolder(response.data?.data)
    }
    fun saveOne(output: ImageOutput) {
        pendingSave=output.file.parentFile?.name
        try { save.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE);type=output.format.mime
            putExtra(Intent.EXTRA_TITLE,"Komprexo-${java.util.UUID.randomUUID()}.${output.format.extension}")
        }) } catch(_: ActivityNotFoundException) { model.notice(FailureCode.FILE_ACCESS) }
    }
    fun share(outputs: List<ImageOutput>) { model.share(outputs) { intent ->
        try { context.startActivity(Intent.createChooser(intent,null)) }
        catch(_: ActivityNotFoundException) { model.notice(FailureCode.FILE_ACCESS) }
    } }
    if(showSettings) ResponsiveSettingsDialog(stringResource(R.string.tool_settings),
        onDismiss={ showSettings=false },onConfirm={ showSettings=false }) {
            if(state.workflow!=Workflow.CONVERT) PresetChoices(state.settings,!state.busy,if(compression) Preset.entries else Preset.entries.filter { it!=Preset.DOCUMENT },model::edit)
            if(compression) {
                TargetControls(state.settings,!state.busy,model::edit)
                Text(stringResource(R.string.compression_settings),style=MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    CompressionMode.entries.forEach { mode -> ChoiceChip(state.settings.mode==mode,{ model.edit(state.settings.copy(mode=mode)) },enabled=!state.busy,
                        label={ Text(stringResource(if(mode==CompressionMode.QUALITY_FIRST) R.string.quality_first else R.string.balanced)) },modifier=Modifier.heightIn(min=48.dp)) }
                }
            }
            FormatControls(state.settings,!state.busy,state.workflow!=Workflow.CONVERT,false,model::edit)
            if(state.workflow!=Workflow.CONVERT) ResizeControls(state.settings,!state.busy,model::edit)
    }
    Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={
        WorkflowTopBar(stringResource(when(state.workflow) { Workflow.BATCH->R.string.home_batch;Workflow.CONVERT->R.string.home_convert;Workflow.RESIZE->R.string.home_resize }),!state.busy,onHome)
    },bottomBar={
        Surface(shadowElevation=4.dp) {
            Column {
            if(state.busy) ProcessingStatus(state.progress)
            FlowRow(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(state.busy) OutlinedButton(model::cancel,modifier=Modifier.heightIn(min=48.dp).testTag("phase2Cancel")) { Text(stringResource(R.string.cancel)) }
                else {
                    if(state.outputs.isEmpty()) Button(model::start,enabled=state.selection.isNotEmpty() && (batch || state.selection.first().source!=null),modifier=Modifier.heightIn(min=48.dp).testTag("phase2Start")) { Text(stringResource(R.string.process_images)) }
                    if(state.outputs.isNotEmpty()) {
                        Button({ if(batch) {
                            try { folder.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) }
                            catch(_: ActivityNotFoundException) { model.notice(FailureCode.FILE_ACCESS) }
                        } else saveOne(state.outputs.single()) },modifier=Modifier.heightIn(min=48.dp).testTag("phase2Save")) { Text(stringResource(if(batch) R.string.save_all else R.string.save)) }
                        OutlinedButton({ share(state.outputs) },modifier=Modifier.heightIn(min=48.dp).testTag("phase2Share")) { Text(stringResource(if(batch) R.string.share_all else R.string.share)) }
                    }
                }
            }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).padding(horizontal=16.dp).testTag("phase2List"),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=12.dp)) {
            item {
                Button({ if(batch) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else single.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },enabled=!state.busy,
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("phase2Select")) { Text(stringResource(if(batch) R.string.select_images else R.string.select_image)) }
                Text(stringResource(R.string.selection_count,state.selection.size,if(batch) MAX_BATCH_IMAGES else 1),modifier=Modifier.testTag("selectionCount"))
                if(compression) QuotaIndicator(state.busy,model.quota,onUpgrade)
                if(!compression) {
                    Row {
                        val batchLabel=stringResource(R.string.batch_operation)
                        Checkbox(state.multiple,model::setMultiple,enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("batchOperation").semantics { contentDescription=batchLabel })
                        Text(stringResource(R.string.batch_operation),modifier=Modifier.padding(top=12.dp))
                    }
                }
                state.restriction?.takeIf { !it.invitesUpgrade() }?.let { Text(stringResource(restrictionLabel(it)),color=MaterialTheme.colorScheme.error) }
                if(state.selection.isNotEmpty()) TextButton(model::clear,enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("clearSelection")) { Text(stringResource(R.string.clear_selection)) }
                state.error?.let { Text(stringResource(errorString(it)),color=MaterialTheme.colorScheme.error) }
                state.progress?.takeIf { !state.busy }?.let { progress ->
                    LinearProgressIndicator(progress={ progress.percent/100f },modifier=Modifier.fillMaxWidth())
                    Text(stringResource(R.string.batch_progress,progress.completed,progress.failed,progress.total,progress.percent),modifier=Modifier.testTag("batchProgress"))
                    progress.current?.let { Text(stringResource(R.string.current_image,it,progress.total)) }
                }
                if(state.exports.isNotEmpty()) Text(stringResource(R.string.export_summary,state.exports.count { it.destination!=null },state.exports.count { it.error!=null }))
                state.exports.filter { it.error!=null }.forEach { Text(stringResource(R.string.image_number,state.outputs.indexOf(it.output)+1)+": "+stringResource(errorString(it.error!!)),color=MaterialTheme.colorScheme.error) }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        if(compression) {
                            Text("${state.settings.targetKiB} KB · ${if(state.settings.mode==CompressionMode.QUALITY_FIRST) stringResource(R.string.quality_first) else stringResource(R.string.balanced)}")
                            Text(stringResource(presetLabel(state.settings.preset)))
                        }
                        Text(stringResource(R.string.output_format)+": "+if(state.settings.format==OutputFormat.AUTO) stringResource(R.string.auto_recommended) else state.settings.format.name)
                        if(state.workflow!=Workflow.CONVERT) Text(when(state.settings.resizeChoice) {
                            ResizeChoice.ORIGINAL -> stringResource(R.string.keep_dimensions)
                            ResizeChoice.PERCENT -> state.settings.percent+"%"
                            ResizeChoice.FIT -> "${state.settings.width} × ${state.settings.height} · "+stringResource(R.string.fit_no_crop)
                            ResizeChoice.CUSTOM -> "${state.settings.width.ifBlank { "—" }} × ${state.settings.height.ifBlank { "—" }} · "+stringResource(if(state.settings.lockAspect) R.string.aspect_lock else R.string.custom_dimensions)
                        })
                        OutlinedButton({ showSettings=true },enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("toolSettings")) { Text(stringResource(R.string.tool_settings)) }
                        if(state.outputs.isNotEmpty() && !state.busy) Button(model::start,
                            modifier=Modifier.heightIn(min=48.dp).testTag("phase2Start")) { Text(stringResource(R.string.process_images)) }
                    }
                }
                if(state.settings.format==OutputFormat.JPEG && state.selection.any { it.thumbnail?.hasAlpha()==true })
                    AlphaConsent(state.settings,!state.busy,model::edit)
            }
            if(state.workflow==Workflow.RESIZE) item { Text(stringResource(R.string.resize_independent),style=MaterialTheme.typography.bodySmall) }
            itemsIndexed(visible,key={ _,item->item.value.id }) { position,indexed ->
                val index=indexed.index
                val item=indexed.value
                val result=resultOf(item)
                if(grouped && (position==0 || group(visible[position-1].value)!=group(item))) Text(stringResource(when(group(item)) {
                    0 -> R.string.successful_results
                    1 -> R.string.failed_images
                    else -> R.string.cancelled_images
                }),style=MaterialTheme.typography.titleMedium)
                OutlinedCard(Modifier.fillMaxWidth().testTag("selection$index")) {
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            item.thumbnail?.let { Image(it.asImageBitmap(),stringResource(R.string.image_number,index+1),Modifier.size(80.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.image_number,index+1),style=MaterialTheme.typography.titleMedium)
                                item.source?.let { source ->
                                    val visual=source.visualDimensions()
                                    Text("${sizeLabel(source.bytes)} · ${visual.width} × ${visual.height} · ${source.mime.substringAfter('/')}")
                                }
                            }
                        }
                        if(result !is ItemResult.Failed) item.error?.let { Text(stringResource(errorString(it)),color=MaterialTheme.colorScheme.error) }
                        OutlinedButton({ model.remove(item.id) },enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("remove$index")) { Text(stringResource(R.string.remove_image)) }
                        when(result) {
                            is ItemResult.Failed -> Text(stringResource(errorString(result.code)),color=MaterialTheme.colorScheme.error)
                            ItemResult.Cancelled -> Text(stringResource(R.string.error_cancelled))
                            is ItemResult.Success -> ResultSummary(result.output,item.source)
                            else -> Unit
                        }
                        val output=(result as? ItemResult.Success)?.output
                        if(output!=null && batch) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ saveOne(output) },enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.save)) }
                            OutlinedButton({ share(listOf(output)) },enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.share)) }
                        }
                    }
                }
            }
            if(!batch && state.outputs.isNotEmpty()) item { ResultSummary(state.outputs.single(),state.selection.firstOrNull()?.source) }
            item { Text(stringResource(R.string.privacy_short),style=MaterialTheme.typography.bodySmall) }
        }
    }
}
@Composable
private fun ResultSummary(output: ImageOutput, source: ImageSource?) {
    Text(stringResource(R.string.output_result),style=MaterialTheme.typography.titleMedium)
    Text(stringResource(if(output.bytes<=output.originalBytes) R.string.output_summary else R.string.output_increase,sizeLabel(output.originalBytes),sizeLabel(output.bytes),String.format(Locale.getDefault(),"%.1f",kotlin.math.abs(output.reductionPercent))))
    val original=source?.visualDimensions()
    if(original!=null) Text(stringResource(R.string.dimension_comparison,original.width,original.height,output.width,output.height,output.format.name))
    else Text("${output.width} × ${output.height} · ${output.format.name}")
    output.targetBytes?.let { Text(stringResource(if(output.meetsTarget==true) R.string.target_met else R.string.target_not_met,sizeLabel(it))) }
    if(output.transparencyRemoved) Text(stringResource(R.string.jpeg_alpha))
}
