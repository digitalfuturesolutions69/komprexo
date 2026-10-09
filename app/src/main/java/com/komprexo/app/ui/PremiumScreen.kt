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
import com.komprexo.app.billing.*
import kotlinx.coroutines.launch
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
fun PremiumScreen(reason: Restriction? = null, busy: Boolean = false, controller: BillingController = BillingServices.controller, onBuy: (() -> Unit)? = null, onBack: ()->Unit) {
    val billing by controller.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Scaffold(contentWindowInsets=WindowInsets.safeDrawing,topBar={
        WorkflowTopBar(stringResource(R.string.premium_title))
    },bottomBar={
        Surface(shadowElevation=4.dp) {
            FlowRow(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Button({ if(onBuy != null) onBuy() else (context as? android.app.Activity)?.let { activity -> if(controller === BillingServices.controller) BillingServices.buy(activity) } },enabled=billing.canBuy && !busy,modifier=Modifier.widthIn(min=132.dp,max=320.dp).heightIn(min=48.dp).testTag("purchasePremium")) { Text(stringResource(if(billing.canBuy) R.string.buy_premium else R.string.purchase_unavailable)) }
                TextButton(onBack,modifier=Modifier.heightIn(min=48.dp).testTag("dismissPremium")) { Text(stringResource(R.string.return_to_tool)) }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("premiumContent"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            reason?.let { Text(stringResource(restrictionLabel(it)),color=MaterialTheme.colorScheme.primary) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(billing.price?.let { stringResource(R.string.play_price_once,it) } ?: stringResource(R.string.premium_price),style=MaterialTheme.typography.headlineSmall)
                    Text(stringResource(if(billing.price == null) R.string.price_provisional else R.string.purchase_disclosure),style=MaterialTheme.typography.bodySmall)
                }
            }
            listOf(R.string.benefit_unlimited,R.string.benefit_batch,R.string.benefit_presets,R.string.benefit_future_no_ads,R.string.benefit_quality).forEach {
                Text("✓  "+stringResource(it))
            }
            Text(stringResource(billingStatusLabel(billing.status)),modifier=Modifier.testTag("billingStatus"))
            OutlinedButton({ if(controller === BillingServices.controller) BillingServices.refresh(restore=true) else scope.launch { controller.refresh(restore=true) } },enabled=!billing.busy && !busy,modifier=Modifier.heightIn(min=48.dp).testTag("restorePurchases")) { Text(stringResource(R.string.restore_purchases)) }
            Text(stringResource(if(billing.price==null) R.string.premium_purchase_notice else R.string.purchase_disclosure),style=MaterialTheme.typography.bodyLarge,modifier=Modifier.testTag("premiumNotice"))
            Text("komprexo.support@gmail.com",style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.free_plan_summary))
            EntitlementTestingControls(!busy)
            Text(stringResource(R.string.privacy_short),style=MaterialTheme.typography.bodySmall)
        }
    }
}

fun billingStatusLabel(status: BillingStatus) = when(status) {
    BillingStatus.LOADING -> R.string.billing_loading
    BillingStatus.READY -> R.string.billing_ready
    BillingStatus.PROCESSING -> R.string.billing_processing
    BillingStatus.PENDING -> R.string.billing_pending
    BillingStatus.PURCHASED -> R.string.billing_purchased
    BillingStatus.ACTIVE -> R.string.billing_active
    BillingStatus.CANCELLED -> R.string.billing_cancelled
    BillingStatus.FAILED -> R.string.billing_failed
    BillingStatus.UNAVAILABLE -> R.string.billing_unavailable
    BillingStatus.RESTORED -> R.string.billing_restored
    BillingStatus.NOT_OWNED -> R.string.billing_not_owned
    BillingStatus.ACKNOWLEDGMENT_PENDING -> R.string.billing_ack_pending
    BillingStatus.OFFLINE_CACHED -> R.string.billing_offline_cached
    BillingStatus.REVOKED -> R.string.billing_revoked
}
