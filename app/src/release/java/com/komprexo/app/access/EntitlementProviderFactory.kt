package com.komprexo.app.access

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Until verified Billing is separately authorized, production is always Free. */
object EntitlementProviderFactory {
    val provider: EntitlementProvider = object : EntitlementProvider {
        override val state = MutableStateFlow<EntitlementState>(EntitlementState.Free).asStateFlow()
    }
}
@Composable fun EntitlementTestingControls(enabled: Boolean) { }
