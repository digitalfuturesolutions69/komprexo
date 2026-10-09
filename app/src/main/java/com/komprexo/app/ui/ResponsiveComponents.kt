package com.komprexo.app.ui

import android.view.WindowManager
import android.os.Build
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.komprexo.app.R
import com.komprexo.app.compression.*
import com.komprexo.app.processing.Preset

/** Dialog chrome stays outside the scroll viewport; IME reduces usable constraints. */
@Composable
fun ResponsiveSettingsDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val dark = isSystemInDarkTheme()
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                val bars = WindowInsetsControllerCompat(window, window.decorView)
                bars.isAppearanceLightStatusBars = !dark
                bars.isAppearanceLightNavigationBars = !dark && Build.VERSION.SDK_INT >= 26
                window.statusBarColor = if(dark) android.graphics.Color.rgb(27,27,27) else android.graphics.Color.rgb(245,245,245)
                window.navigationBarColor = if(dark || Build.VERSION.SDK_INT < 26) android.graphics.Color.rgb(27,27,27)
                    else android.graphics.Color.rgb(245,245,245)
            }
        }
        val density=LocalDensity.current
        val statusHeight=WindowInsets.statusBars.getTop(density).toFloat()
        val navigationHeight=WindowInsets.navigationBars.getBottom(density).toFloat()
        val statusBackground=if(dark) Color(0xff1b1b1b) else Color(0xfff5f5f5)
        val navigationBackground=if(dark || Build.VERSION.SDK_INT < 26) Color(0xff1b1b1b) else Color(0xfff5f5f5)
        Box(Modifier.fillMaxSize()) {
        // API 35+ draws transparent system bars: explicitly paint their inset areas.
        Canvas(Modifier.matchParentSize()) {
            drawRect(statusBackground,size=Size(size.width,statusHeight))
            drawRect(navigationBackground,topLeft=Offset(0f,size.height-navigationHeight),size=Size(size.width,navigationHeight))
        }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(12.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center) {
            Box(Modifier.matchParentSize().testTag("settingsBackdrop").pointerInput(onDismiss) { detectTapGestures { onDismiss() } })
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight * 0.9f)
                .testTag("settingsDialog"), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                        .testTag("settingsScroll").padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        content()
                    }
                    HorizontalDivider()
                    TextButton(onClick = { focus.clearFocus(); keyboard?.hide(); onConfirm() },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.End).heightIn(min = 48.dp).testTag("settingsDone")) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
        }
    }
}

/** Wrapping title and separate Home action, with no single-line title truncation. */
@Composable
fun WorkflowTopBar(title: String, enabled: Boolean = true, onHome: (() -> Unit)? = null, subtitle: String? = null) {
    Surface {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("pageTitle").semantics { heading() })
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            if (onHome != null) TextButton(onHome, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp).testTag("homeNavigation")) {
                Text(stringResource(R.string.home))
            }
        }
    }
}

@Composable
fun ChoiceChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val checkColor=MaterialTheme.colorScheme.primary
    Surface(modifier=modifier.heightIn(min=48.dp).selectable(selected,onClick=onClick,enabled=enabled,role=Role.RadioButton),
        shape=MaterialTheme.shapes.small,
        color=if(selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor=if(enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f),
        border=BorderStroke(1.dp,if(selected) checkColor else MaterialTheme.colorScheme.outline)) {
        Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            if(selected) Canvas(Modifier.size(18.dp).clearAndSetSemantics {}) {
                drawLine(checkColor,Offset(size.width * 0.15f,size.height * 0.5f),Offset(size.width * 0.4f,size.height * 0.75f),size.width * 0.12f,StrokeCap.Round)
                drawLine(checkColor,Offset(size.width * 0.4f,size.height * 0.75f),Offset(size.width * 0.85f,size.height * 0.2f),size.width * 0.12f,StrokeCap.Round)
            }
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { label() }
        }
    }
}

// UI-only saver: keep editable compression resize controls across recreation/navigation.
val EditableSettingsSaver = listSaver<EditableSettings, String>(
    save = { listOf(it.preset.name, it.targetKiB, it.customTarget.toString(), it.format.name, it.mode.name,
        it.resizeChoice.name, it.percent, it.width, it.height, it.lockAspect.toString(), it.allowAlphaRemoval.toString()) },
    restore = { EditableSettings(Preset.valueOf(it[0]), it[1], it[2].toBoolean(), OutputFormat.valueOf(it[3]),
        CompressionMode.valueOf(it[4]), ResizeChoice.valueOf(it[5]), it[6], it[7], it[8], it[9].toBoolean(), it[10].toBoolean()) })

@Composable
fun ProcessingStatus(progress: com.komprexo.app.processing.BatchProgress? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if(progress == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.processing))
        } else {
            LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
            val description = stringResource(R.string.batch_progress, progress.completed, progress.failed, progress.total, progress.percent)
            Text("${progress.completed + progress.failed}/${progress.total} · ${progress.percent}%",
                modifier = Modifier.testTag("processingStatus").semantics { contentDescription = description })
        }
    }
}
