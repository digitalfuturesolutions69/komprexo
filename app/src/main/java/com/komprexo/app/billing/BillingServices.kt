package com.komprexo.app.billing

import android.app.Activity
import android.content.Context
import com.komprexo.app.access.EntitlementState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object BillingServices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow<EntitlementState>(EntitlementState.Free)
    val entitlement = mutable.asStateFlow()
    lateinit var controller: BillingController
        private set
    private lateinit var transport: PlayBillingTransport
    private var refreshJob: Job? = null
    @Synchronized fun initialize(context: Context) {
        if (::controller.isInitialized) return
        transport = PlayBillingTransport(context, { code -> scope.launch { controller.callback(code) } }, { refresh() })
        controller = BillingController(transport, SealedOwnershipStore(context))
        scope.launch { controller.state.collect { mutable.value = it } }
        scope.launch {
            while (true) { controller.expireCache(); delay(15_000) }
        }
        refresh()
    }
    fun refresh(restore: Boolean = false) {
        if (!::controller.isInitialized || refreshJob?.isActive == true) return
        refreshJob = scope.launch { controller.refresh(restore = restore) }
    }
    fun buy(activity: Activity) { scope.launch { controller.buy { transport.launch(activity, it) } } }
}
