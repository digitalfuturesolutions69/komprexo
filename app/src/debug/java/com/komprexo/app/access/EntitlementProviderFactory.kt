package com.komprexo.app.access

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komprexo.app.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ephemeral test entitlement; no payment or persisted unlock. File absent from release. */
object EntitlementProviderFactory {
    private var testing = false
    private val mutable = MutableStateFlow<EntitlementState>(EntitlementState.Free)
    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            com.komprexo.app.billing.BillingServices.entitlement.collect { if (!testing) mutable.value = it }
        }
    }
    val provider: EntitlementProvider = object : EntitlementProvider { override val state = mutable.asStateFlow() }
    fun setTestingPremium(active: Boolean) { testing = active; mutable.value = if (active) EntitlementState.Premium else com.komprexo.app.billing.BillingServices.entitlement.value }
}
@Composable fun EntitlementTestingControls(enabled: Boolean) {
    val plan by EntitlementProviderFactory.provider.state.collectAsStateWithLifecycle()
    OutlinedButton({ EntitlementProviderFactory.setTestingPremium(plan != EntitlementState.Premium) }, enabled = enabled,
        modifier = Modifier.heightIn(min = 48.dp).testTag("debugEntitlement")) {
        Text(stringResource(if (plan == EntitlementState.Premium) R.string.debug_free else R.string.debug_premium))
    }
}
