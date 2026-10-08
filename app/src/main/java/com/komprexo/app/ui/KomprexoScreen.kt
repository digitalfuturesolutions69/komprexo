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
import java.util.Locale

private val presets = listOf(100L * 1024, 200L * 1024, 300L * 1024, 500L * 1024, 1024L * 1024, 2L * 1024 * 1024)

@Composable
fun KomprexoScreen(model: CompressionViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var target by rememberSaveable { mutableStateOf(200L * 1024) }
    var custom by rememberSaveable { mutableStateOf("") }
    var formatName by rememberSaveable { mutableStateOf(OutputFormat.AUTO.name) }
    val format = OutputFormat.valueOf(formatName)
    val customBytes = custom.toLongOrNull()?.takeIf { it in 1..10240 }?.times(1024)
    val bytes = if (target == 0L) customBytes else target
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { model.select(it) }
    // MIME follows each result, including alpha-preserving Auto WebP.
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        model.save(if (response.resultCode == android.app.Activity.RESULT_OK) response.data?.data else null)
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xff356858))) {
        Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color(0xfff6f5f0)) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.intro))
                Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !state.busy) { Text(stringResource(R.string.select_image)) }
                Text(stringResource(R.string.maximum_size), style = MaterialTheme.typography.titleMedium)
                presets.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { value -> FilterChip(selected = target == value, enabled = !state.busy, onClick = { target = value }, label = { Text(sizeLabel(value)) }) }
                    }
                }
                FilterChip(selected = target == 0L, enabled = !state.busy, onClick = { target = 0L }, label = { Text(stringResource(R.string.custom)) })
                if (target == 0L) OutlinedTextField(value = custom, onValueChange = { custom = it.take(5) }, enabled = !state.busy,
                    label = { Text(stringResource(R.string.custom_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = bytes == null, supportingText = { Text(stringResource(R.string.custom_hint)) }, singleLine = true)
                Text(stringResource(R.string.output_format), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutputFormat.entries.forEach { item -> FilterChip(selected = format == item, enabled = !state.busy, onClick = { formatName = item.name }, label = { Text(if (item == OutputFormat.AUTO) stringResource(R.string.auto_format) else item.name) }) }
                }
                if (format == OutputFormat.JPEG) Text(stringResource(R.string.jpeg_alpha))
                Button(onClick = { bytes?.let { model.compress(it, format) } }, enabled = state.source != null && bytes != null && !state.busy) { Text(stringResource(R.string.compress)) }
                if (state.busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.processing))
                    Text(stringResource(R.string.dimensions_progress, state.progress?.width ?: 0, state.progress?.height ?: 0))
                    OutlinedButton(onClick = model::cancel) { Text(stringResource(R.string.cancel)) }
                }
                state.error?.let { Text(stringResource(errorString(it)), color = MaterialTheme.colorScheme.error) }
                if (state.saved) Text(stringResource(R.string.saved))
                state.source?.let { source ->
                    Text(stringResource(R.string.original), style = MaterialTheme.typography.titleMedium)
                    Text(sizeLabel(source.bytes))
                    state.originalPreview?.let { Image(it.asImageBitmap(), stringResource(R.string.original), Modifier.fillMaxWidth().height(220.dp)) }
                }
                state.result?.let { result ->
                    HorizontalDivider()
                    Text(stringResource(R.string.compressed), style = MaterialTheme.typography.titleMedium)
                    state.outputPreview?.let { Image(it.asImageBitmap(), stringResource(R.string.compressed), Modifier.fillMaxWidth().height(220.dp)) }
                    Text(stringResource(R.string.result_summary, sizeLabel(result.bytes), String.format(Locale.getDefault(), "%.1f", result.reductionPercent), result.width, result.height, result.format.name))
                    Text(stringResource(if (result.meetsTarget) R.string.target_met else R.string.target_not_met, sizeLabel(result.maxBytes)))
                    if (result.transparencyRemoved) Text(stringResource(R.string.jpeg_alpha))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(enabled = !state.busy, onClick = {
                            try {
                                save.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = result.format.mime
                                    putExtra(Intent.EXTRA_TITLE, "Komprexo.${result.format.extension}")
                                })
                            } catch (_: ActivityNotFoundException) { model.notice(FailureCode.FILE_ACCESS) }
                        }) { Text(stringResource(R.string.save)) }
                        OutlinedButton(enabled = !state.busy, onClick = { model.share { intent -> context.startActivity(Intent.createChooser(intent, null)) } }) { Text(stringResource(R.string.share)) }
                    }
                }
                Text(stringResource(R.string.privacy_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
fun sizeLabel(bytes: Long): String = if (bytes >= 1024 * 1024) String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024)) else String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
fun errorString(code: FailureCode): Int = when (code) {
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
