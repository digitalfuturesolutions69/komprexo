package com.komprexo.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.komprexo.app.R
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import java.util.Locale

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.komprexo.app.MainActivity

private val presets = listOf(100L * 1024, 200L * 1024, 300L * 1024, 500L * 1024, 1024L * 1024, 2L * 1024 * 1024)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KomprexoScreen(model: CompressionViewModel = viewModel(), onHome: (() -> Unit)? = null, onUpgrade: ()->Unit = {}) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    SideEffect { (context as? MainActivity)?.applySystemBars(dark) }
    var presetDialog by rememberSaveable { mutableStateOf(false) }
    var resizeSettings by rememberSaveable(stateSaver = EditableSettingsSaver) { mutableStateOf(EditableSettings()) }
    var target by rememberSaveable { mutableLongStateOf(200L * 1024) }
    var custom by rememberSaveable { mutableStateOf("") }
    var formatName by rememberSaveable { mutableStateOf(OutputFormat.AUTO.name) }
    var modeName by rememberSaveable { mutableStateOf(CompressionMode.QUALITY_FIRST.name) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    var before by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.result?.file) { reviewing = state.result != null; before = false }
    if (presetDialog) ResponsiveSettingsDialog(stringResource(R.string.smart_presets),
        onDismiss = { presetDialog = false }, onConfirm = {
            try { resizeSettings.resize(); presetDialog = false } catch (e: ImageProblem) { model.notice(e.code) }
        }) {
        PresetChoices(resizeSettings, !state.busy) { settings ->
            if (!model.requestPreset(settings.preset)) return@PresetChoices
            resizeSettings = settings
            val options = settings.options(); target = options.targetBytes; formatName = options.format.name; modeName = options.mode.name
        }
        ResizeControls(resizeSettings, !state.busy) { resizeSettings = it }
    }
    val format = OutputFormat.valueOf(formatName)
    val mode = CompressionMode.valueOf(modeName)
    val bytes = if (target == 0L) custom.toLongOrNull()?.takeIf { it in 1..10240 }?.times(1024) else target
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { model.select(it) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        model.save(if (response.resultCode == Activity.RESULT_OK) response.data?.data else null)
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xffa1d3bc)) else lightColorScheme(primary = Color(0xff356858))) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { WorkflowTopBar(stringResource(R.string.app_name), !state.busy, onHome, stringResource(R.string.home_compress)) },
            bottomBar = {
                Surface(shadowElevation = 4.dp) {
                    Column {
                    if(state.busy) ProcessingStatus()
                    FlowRow(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val result = state.result
                        if (reviewing && result != null) {
                            Button(modifier = Modifier.heightIn(min = 48.dp).testTag("saveAction"), enabled = !state.busy, onClick = {
                                try { save.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE); type = result.format.mime
                                    putExtra(Intent.EXTRA_TITLE, "Komprexo.${result.format.extension}")
                                }) } catch (_: ActivityNotFoundException) { model.notice(FailureCode.FILE_ACCESS) }
                            }) { Text(stringResource(R.string.save)) }
                            OutlinedButton(modifier = Modifier.heightIn(min = 48.dp).testTag("shareAction"), enabled = !state.busy, onClick = { model.share { context.startActivity(Intent.createChooser(it, null)) } }) { Text(stringResource(R.string.share)) }
                            if (state.busy) OutlinedButton(onClick = model::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
                        } else if (state.busy) {
                            OutlinedButton(onClick = model::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
                        } else {
                            Button(modifier = Modifier.heightIn(min = 48.dp), enabled = state.source != null && bytes != null, onClick = { bytes?.let { try { model.compress(it, format, mode, resizeSettings.resize(), resizeSettings.preset) } catch (e: ImageProblem) { model.notice(e.code) } } }) { Text(stringResource(R.string.compress)) }
                            if (result != null) OutlinedButton(onClick = { reviewing = true }) { Text(stringResource(R.string.view_result)) }
                        }
                    }
                    }
                }
            }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.select_image)) }
                Text(stringResource(R.string.selection_count, if(state.source != null) 1 else 0, 1))
                QuotaIndicator(state.busy,model.quota,onUpgrade)
                state.restriction?.takeIf { !it.invitesUpgrade() }?.let { Text(stringResource(restrictionLabel(it)),color=MaterialTheme.colorScheme.error) }
                state.error?.let { Text(stringResource(errorString(it)), color = MaterialTheme.colorScheme.error) }
                if (state.saved) Text(stringResource(R.string.saved))
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.processing))
                    state.progress?.let { Text(stringResource(R.string.dimensions_progress, it.width, it.height)) }
                }
                val result = state.result
                val source = state.source
                if (reviewing && result != null && source != null) {
                    Text(stringResource(R.string.compressed), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.size_comparison, sizeLabel(source.bytes), sizeLabel(result.bytes), String.format(Locale.getDefault(), "%.1f", result.reductionPercent)))
                    val rotated = source.orientation in 5..8
                    Text(stringResource(R.string.dimension_comparison, if (rotated) source.height else source.width, if (rotated) source.width else source.height, result.width, result.height, result.format.name))
                    Text(stringResource(if (result.meetsTarget) R.string.target_met else R.string.target_not_met, sizeLabel(result.maxBytes)))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip(before, { before = true }, label = { Text(stringResource(R.string.original)) }, modifier = Modifier.heightIn(min = 48.dp))
                        ChoiceChip(!before, { before = false }, label = { Text(stringResource(R.string.after)) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                    val preview = if (before) state.originalPreview else state.outputPreview
                    preview?.let { Image(it.asImageBitmap(), stringResource(if (before) R.string.original else R.string.compressed), Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp).testTag("comparisonPreview")) }
                    if (result.transparencyRemoved) Text(stringResource(R.string.jpeg_alpha))
                    OutlinedButton(onClick = { reviewing = false }, enabled = !state.busy) { Text(stringResource(R.string.adjust_settings)) }
                } else {
                    source?.let {
                        Text("${sizeLabel(it.bytes)} · ${it.width} × ${it.height}")
                        state.originalPreview?.let { image -> Image(image.asImageBitmap(), stringResource(R.string.original), Modifier.fillMaxWidth().height(96.dp)) }
                    }
                    TextButton(onClick = { presetDialog = true }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.smart_presets)) }
                    Text(stringResource(R.string.maximum_size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { value -> ChoiceChip(target == value, { target = value }, enabled = !state.busy, label = { Text(if (value >= 1024 * 1024) "${value / (1024 * 1024)} MB" else "${value / 1024} KB") }, modifier = Modifier.heightIn(min = 48.dp)) }
                        ChoiceChip(target == 0L, { target = 0L }, enabled = !state.busy, label = { Text(stringResource(R.string.custom)) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                    if (target == 0L) OutlinedTextField(custom, { custom = it.take(5) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("customSize"), label = { Text(stringResource(R.string.custom_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = bytes == null, supportingText = { Text(stringResource(R.string.custom_hint)) }, singleLine = true)
                    Text(stringResource(R.string.compression_settings), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionMode.entries.forEach { item -> ChoiceChip(mode == item, { modeName = item.name }, enabled = !state.busy, label = { Text(stringResource(if (item == CompressionMode.QUALITY_FIRST) R.string.quality_first else R.string.balanced)) }, modifier = Modifier.heightIn(min = 48.dp)) }
                    }
                    Text(stringResource(R.string.output_format))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutputFormat.entries.forEach { item -> ChoiceChip(format == item, { formatName = item.name }, enabled = !state.busy, label = { Text(if (item == OutputFormat.AUTO) stringResource(R.string.auto_recommended) else item.name) }, modifier = Modifier.heightIn(min = 48.dp)) }
                    }
                    if (format == OutputFormat.JPEG) Text(stringResource(R.string.jpeg_alpha))
                }
                Text(stringResource(R.string.privacy_short), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
fun sizeLabel(bytes: Long): String = if (bytes >= 1024 * 1024) String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024)) else String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
fun errorString(code: FailureCode): Int = when (code) {
    FailureCode.INVALID_DIMENSIONS -> R.string.error_dimensions
    FailureCode.DEVICE_LIMIT -> R.string.error_device_limit
    FailureCode.BATCH_LIMIT -> R.string.error_batch_limit
    FailureCode.BUSY -> R.string.error_busy
    FailureCode.ALPHA_CONFIRMATION -> R.string.error_alpha_confirmation
    FailureCode.OUTPUT_TOO_LARGE -> R.string.error_output_large
    FailureCode.INVALID_URI -> R.string.error_uri
    FailureCode.READ_PERMISSION -> R.string.error_permission
    FailureCode.READ_FAILED -> R.string.error_read
    FailureCode.DECODER_FAILED -> R.string.error_decode
    FailureCode.ENCODER_FAILED -> R.string.error_encode
    FailureCode.OUTPUT_INVALID -> R.string.error_output
    FailureCode.UNSUPPORTED_HEIF -> R.string.error_heif
    FailureCode.INVALID_IMAGE -> R.string.error_invalid
    FailureCode.CORRUPT_IMAGE -> R.string.error_corrupt
    FailureCode.UNSUPPORTED_FORMAT -> R.string.error_format
    FailureCode.INPUT_TOO_LARGE -> R.string.error_large
    FailureCode.INSUFFICIENT_MEMORY -> R.string.error_memory
    FailureCode.UNREACHABLE_TARGET -> R.string.error_target
    FailureCode.FILE_ACCESS -> R.string.error_access
    FailureCode.STORAGE_FULL -> R.string.error_storage
    FailureCode.CANCELLED -> R.string.error_cancelled
    FailureCode.INTERRUPTED -> R.string.error_interrupted
}
