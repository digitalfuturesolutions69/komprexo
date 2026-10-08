package com.komprexo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KomprexoApp(compression: CompressionViewModel = viewModel(), phase2: Phase2ViewModel = viewModel()) {
    var route by rememberSaveable { mutableStateOf("home") }
    val single by compression.state.collectAsStateWithLifecycle()
    val multi by phase2.state.collectAsStateWithLifecycle()
    val dark=isSystemInDarkTheme()
    val context=LocalContext.current
    SideEffect { (context as? MainActivity)?.applySystemBars(dark) }
    BackHandler(route!="home") {
        if(single.busy) compression.cancel() else if(multi.busy) phase2.cancel() else route="home"
    }
    MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=Color(0xffa1d3bc)) else lightColorScheme(primary=Color(0xff356858))) {
        when(route) {
            "compress" -> KomprexoScreen(compression,onHome={ route="home" })
            "home" -> Scaffold(contentWindowInsets=WindowInsets.safeDrawing) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.app_name),style=MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.home_description))
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        listOf("compress" to R.string.home_compress,"BATCH" to R.string.home_batch,"CONVERT" to R.string.home_convert,"RESIZE" to R.string.home_resize).forEach { (next,label) ->
                            Button({ if(next!="compress") phase2.enter(Workflow.valueOf(next));route=next },enabled=!single.busy && !multi.busy,modifier=Modifier.widthIn(min=132.dp).heightIn(min=64.dp).testTag("home$next")) { Text(stringResource(label)) }
                        }
                    }
                    if(single.busy || multi.busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        OutlinedButton({ compression.cancel();phase2.cancel() },modifier=Modifier.heightIn(min=48.dp)) { Text(stringResource(R.string.cancel)) }
                    }
                    Text(stringResource(R.string.privacy_short),style=MaterialTheme.typography.bodySmall)
                }
            }
            else -> Phase2Screen(phase2,onHome={ route="home" })
        }
    }
}
