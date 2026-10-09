package com.komprexo.app.access

import androidx.compose.runtime.Composable
import com.komprexo.app.billing.BillingServices

/** Production entitlement comes only from Play ownership reconciliation. */
object EntitlementProviderFactory {
    val provider: EntitlementProvider = object : EntitlementProvider {
        override val state = BillingServices.entitlement
    }
}
@Composable fun EntitlementTestingControls(enabled: Boolean) { }
