package com.komprexo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.komprexo.app.MainActivity
import com.komprexo.app.R
import com.komprexo.app.access.Restriction

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KomprexoApp(compression: CompressionViewModel = viewModel(), phase2: Phase2ViewModel = viewModel()) {
    var route by rememberSaveable { mutableStateOf("home") }
    val single by compression.state.collectAsStateWithLifecycle()
    val multi by phase2.state.collectAsStateWithLifecycle()
    val screenState=rememberSaveableStateHolder()
    var premiumReturn by rememberSaveable { mutableStateOf("home") }
    var premiumReason by remember { mutableStateOf<Restriction?>(null) }
    fun premium(reason: Restriction? = null) {
        if(single.busy || multi.busy) return
        if(route!="premium") premiumReturn=route
        premiumReason=reason;route="premium"
    }
    LaunchedEffect(single.restriction,multi.restriction,single.busy,multi.busy) {
        val reason=single.restriction?.takeIf { it.invitesUpgrade() } ?: multi.restriction?.takeIf { it.invitesUpgrade() }
        if(reason!=null && !single.busy && !multi.busy) {
            premium(reason);compression.clearRestriction();phase2.clearRestriction()
        }
    }
    var pendingRoute by rememberSaveable { mutableStateOf<String?>(null) }
    fun navigate(next: String) {
        if(next!="compress") {
            val workflow=Workflow.valueOf(next)
            val defaults=EditableSettings(format=if(multi.workflow==Workflow.CONVERT) com.komprexo.app.compression.OutputFormat.PNG else com.komprexo.app.compression.OutputFormat.AUTO)
            if(multi.workflow!=workflow && (multi.selection.isNotEmpty() || multi.settings!=defaults)) {
                pendingRoute=next
                return
            }
            phase2.enter(workflow)
        }
        route=next
    }
    val dark=isSystemInDarkTheme()
    val context=LocalContext.current
    SideEffect { (context as? MainActivity)?.applySystemBars(dark) }
    BackHandler(route!="home") {
        if(single.busy) compression.cancel() else if(multi.busy) phase2.cancel() else route=if(route=="premium") premiumReturn else "home"
    }
    MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=Color(0xffa1d3bc)) else lightColorScheme(primary=Color(0xff356858))) {
        pendingRoute?.let { next ->
            AlertDialog(onDismissRequest={ pendingRoute=null },title={ Text(stringResource(R.string.switch_tool)) },
                text={ Text(stringResource(R.string.switch_tool_warning)) },
                confirmButton={ TextButton({ phase2.enter(Workflow.valueOf(next));route=next;pendingRoute=null },modifier=Modifier.heightIn(min=48.dp).testTag("confirmSwitch")) { Text(stringResource(R.string.switch_tool)) } },
                dismissButton={ TextButton({ pendingRoute=null },modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.cancel)) } })
        }
        screenState.SaveableStateProvider(route) {
        when(route) {
            "premium" -> PremiumScreen(premiumReason,single.busy || multi.busy,onBack={ route=premiumReturn })
            "compress" -> KomprexoScreen(compression,onHome={ route="home" },onUpgrade={ premium() })
            "home" -> Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={ WorkflowTopBar(stringResource(R.string.app_name)) }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.home_description))
                    QuotaIndicator(single.busy || multi.busy,compression.quota) { premium() }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        listOf("compress" to R.string.home_compress,"BATCH" to R.string.home_batch,"CONVERT" to R.string.home_convert,"RESIZE" to R.string.home_resize).forEach { (next,label) ->
                            Button({ navigate(next) },enabled=!single.busy && !multi.busy,modifier=Modifier.widthIn(min=132.dp,max=560.dp).heightIn(min=64.dp).testTag("home$next")) { Text(stringResource(label)) }
                        }
                    }
                    if(single.busy || multi.busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        OutlinedButton({ compression.cancel();phase2.cancel() },modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.cancel)) }
                    }
                    Text(stringResource(R.string.privacy_short),style=MaterialTheme.typography.bodySmall)
                }
            }
            else -> Phase2Screen(phase2,onHome={ route="home" },onUpgrade={ premium() })
        }
    }
    }
}
