package com.komprexo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.komprexo.app.R
import com.komprexo.app.access.*
import kotlinx.coroutines.delay

fun restrictionLabel(reason: Restriction) = when(reason) {
    Restriction.DAILY_QUOTA -> R.string.quota_exhausted
    Restriction.BATCH_LIMIT -> R.string.free_batch_limit
    Restriction.PREMIUM_PRESET -> R.string.premium_preset_notice
    Restriction.BUSY -> R.string.processing_busy
    Restriction.QUOTA_UNAVAILABLE -> R.string.quota_unavailable
}
fun Restriction.invitesUpgrade() = this in setOf(Restriction.DAILY_QUOTA,Restriction.BATCH_LIMIT,Restriction.PREMIUM_PRESET)

@Composable
fun QuotaIndicator(busy: Boolean, quota: DailyQuotaManager, operation: Operation? = null, onUpgrade: ()->Unit) {
    val snapshot by quota.state.collectAsStateWithLifecycle()
    val plan by quota.entitlement.collectAsStateWithLifecycle()
    val lifecycleOwner=androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(quota,lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while(true) {
                try { quota.refresh() } catch(_: AccessDenied) { /* All Free counters fail closed. */ }
                delay(15_000)
            }
        }
    }
    val remaining=operation?.let(snapshot::remaining)
    val feature=operation?.let { stringResource(when(it) {
        Operation.COMPRESS -> R.string.home_compress
        Operation.RESIZE -> R.string.home_resize
        Operation.CONVERT -> R.string.home_convert
    }) }
    val text=when {
        plan==EntitlementState.Premium -> stringResource(R.string.quota_unlimited)
        snapshot.unavailable -> stringResource(R.string.quota_unavailable)
        snapshot.remaining==null -> stringResource(R.string.quota_loading)
        operation==null -> stringResource(R.string.quota_all_remaining,snapshot.remaining ?: 0,snapshot.resizeRemaining ?: 0,snapshot.convertRemaining ?: 0)
        remaining==0 && !busy -> stringResource(R.string.quota_feature_exhausted,feature ?: "")
        remaining==0 && busy -> stringResource(R.string.processing)
        else -> stringResource(R.string.quota_feature_remaining,feature ?: "",remaining ?: 0)
    }
    Column(Modifier.fillMaxWidth().testTag("quotaIndicator").semantics { liveRegion=LiveRegionMode.Polite }) {
        Text(text)
        if(plan==EntitlementState.Free) TextButton(onUpgrade,enabled=!busy,modifier=Modifier.heightIn(min=48.dp).testTag("upgrade")) { Text(stringResource(R.string.explore_premium)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PremiumScreen(reason: Restriction? = null, busy: Boolean = false, onBack: ()->Unit) {
    Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={
        WorkflowTopBar(stringResource(R.string.premium_title))
    },bottomBar={
        Surface(shadowElevation=4.dp) {
            FlowRow(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Button({},enabled=false,modifier=Modifier.widthIn(min=132.dp,max=320.dp).heightIn(min=48.dp).testTag("purchasePremium")) { Text(stringResource(R.string.purchase_unavailable)) }
                TextButton(onBack,modifier=Modifier.heightIn(min=48.dp).testTag("dismissPremium")) { Text(stringResource(R.string.return_to_tool)) }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("premiumContent"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            reason?.let { Text(stringResource(restrictionLabel(it)),color=MaterialTheme.colorScheme.primary) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.premium_price),style=MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.price_provisional),style=MaterialTheme.typography.bodySmall)
                }
            }
            listOf(R.string.benefit_unlimited,R.string.benefit_batch,R.string.benefit_presets,R.string.benefit_future_no_ads,R.string.benefit_quality).forEach {
                Text("✓  "+stringResource(it))
            }
            Text(stringResource(R.string.premium_purchase_notice),style=MaterialTheme.typography.bodyLarge,modifier=Modifier.testTag("premiumNotice"))
            Text(stringResource(R.string.free_plan_summary))
            EntitlementTestingControls(!busy)
            Text(stringResource(R.string.privacy_short),style=MaterialTheme.typography.bodySmall)
        }
    }
}
