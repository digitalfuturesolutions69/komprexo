package com.komprexo.app.billing

import com.komprexo.app.access.EntitlementProvider
import com.komprexo.app.access.EntitlementState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

/** Decisions depend on successful current Play ownership queries, not purchase callbacks.
 * All transitions are serialized. Transport/ack failures never count as a purchase. */
class BillingController(private val transport: BillingTransport, private val store: OwnershipStore,
    private val now: () -> Long = System::currentTimeMillis, private val pause: suspend (Long) -> Unit = { delay(it) }) : EntitlementProvider {
    private val gate = Mutex()
    private val entitlement = MutableStateFlow<EntitlementState>(EntitlementState.Free)
    override val state = entitlement.asStateFlow()
    private val mutable = MutableStateFlow(BillingUiState())
    val ui = mutable.asStateFlow()
    private var cache: OwnershipCache? = null
    private var loaded = false
    private var offer: PurchaseOffer? = null
    private var processingUntil = 0L
    private val acknowledged = mutableSetOf<String>()

    private fun publish(status: BillingStatus, busy: Boolean = false) {
        val premium = entitlement.value == EntitlementState.Premium
        mutable.value = BillingUiState(status, offer?.price, premium,
            offer != null && !premium && !busy && status !in setOf(BillingStatus.PROCESSING, BillingStatus.PENDING, BillingStatus.ACKNOWLEDGMENT_PENDING), busy)
    }
    private suspend fun restoreCache() {
        if (!loaded) {
            cache = try { store.read() } catch (_: Exception) { null }
            loaded = true
        }
        if (cache?.valid(now()) == true) entitlement.value = EntitlementState.Premium
        else {
            cache = null
            entitlement.value = EntitlementState.Free
            // A rollback/expiry must not revive a stale cache on the next restart.
            try { store.write(null) } catch (_: Exception) { }
        }
    }
    suspend fun expireCache() = gate.withLock {
        restoreCache()
        if (entitlement.value == EntitlementState.Free && mutable.value.premium) publish(BillingStatus.UNAVAILABLE)
    }
    suspend fun refresh(restore: Boolean = false, purchased: Boolean = false) = gate.withLock {
        restoreCache()
        publish(BillingStatus.LOADING, busy = true)
        try {
            var connection = BillingCode.DISCONNECTED
            repeat(3) { attempt ->
                if (connection != BillingCode.OK) {
                    if (attempt > 0) pause(1_000L shl (attempt - 1))
                    connection = try { transport.connect() } catch (_: TimeoutCancellationException) { BillingCode.NETWORK }
                }
            }
            if (connection != BillingCode.OK) { offer = null; publish(if (entitlement.value == EntitlementState.Premium) BillingStatus.OFFLINE_CACHED else BillingStatus.UNAVAILABLE); return@withLock }
            val products = transport.products()
            offer = if (products.code == BillingCode.OK) selectLifetimeOffer(products.value.orEmpty()) else null
            // Product configuration failure must not hide ownership of an existing purchase.
            val reply = transport.purchases()
            if (reply.code != BillingCode.OK) {
                restoreCache()
                offer = null
                publish(if (entitlement.value == EntitlementState.Premium) BillingStatus.OFFLINE_CACHED else BillingStatus.UNAVAILABLE)
                return@withLock
            }
            val relevant = reply.value.orEmpty().filter { PREMIUM_PRODUCT in it.productIds && it.packageMatches && it.quantity == 1 && it.token.isNotBlank() }.distinctBy { it.token }
            val bought = relevant.filter { it.payment == PaymentState.PURCHASED }
            var accepted: OwnedPurchase? = null
            var ackPending = false
            for (purchase in bought) {
                var acked = purchase.acknowledged || purchase.token in acknowledged
                if (!acked) {
                    repeat(3) { attempt ->
                        if (!acked) {
                            if (attempt > 0) pause(1_000L shl (attempt - 1))
                            acked = try { transport.acknowledge(purchase.token) == BillingCode.OK } catch (_: TimeoutCancellationException) { false }
                        }
                    }
                }
                if (acked) { acknowledged.add(purchase.token); accepted = purchase; break }
                ackPending = true
            }
            val wasPremium = entitlement.value == EntitlementState.Premium
            if (accepted != null) {
                val checked = now()
                cache = OwnershipCache(checked, checked + CACHE_LIFETIME_MILLIS,
                    MessageDigest.getInstance("SHA-256").digest(accepted.token.toByteArray()).joinToString("") { "%02x".format(it) })
                // A failed cache write does not revoke a successfully reconciled live purchase.
                try { store.write(cache) } catch (_: Exception) { }
                entitlement.value = EntitlementState.Premium
                processingUntil = 0
                publish(if (restore) BillingStatus.RESTORED else if (purchased) BillingStatus.PURCHASED else BillingStatus.ACTIVE)
            } else {
                cache = null; entitlement.value = EntitlementState.Free
                try { store.write(null) } catch (_: Exception) { }
                publish(when {
                    ackPending -> BillingStatus.ACKNOWLEDGMENT_PENDING
                    relevant.any { it.payment == PaymentState.PENDING } -> BillingStatus.PENDING
                    purchased && now() < processingUntil -> BillingStatus.PROCESSING
                    !restore && now() < processingUntil -> BillingStatus.PROCESSING
                    wasPremium -> BillingStatus.REVOKED
                    restore -> BillingStatus.NOT_OWNED
                    offer != null -> BillingStatus.READY
                    else -> BillingStatus.UNAVAILABLE
                })
            }
        } catch (_: TimeoutCancellationException) {
            restoreCache(); offer = null
            publish(if (entitlement.value == EntitlementState.Premium) BillingStatus.OFFLINE_CACHED else BillingStatus.UNAVAILABLE)
        } catch (e: CancellationException) { publish(BillingStatus.UNAVAILABLE); throw e }
          catch (_: Exception) {
            restoreCache(); offer = null
            publish(if (entitlement.value == EntitlementState.Premium) BillingStatus.OFFLINE_CACHED else BillingStatus.UNAVAILABLE)
        }
    }
    /** Launch callback keeps Activity references outside the long-lived controller. */
    suspend fun buy(launch: (PurchaseOffer) -> BillingCode) {
        val alreadyOwned = gate.withLock {
            if (!ui.value.canBuy) return@withLock false
            val displayed = offer ?: return@withLock false
            publish(BillingStatus.LOADING, busy = true)
            val fresh = try {
                if (transport.connect() != BillingCode.OK) null else {
                    val reply = transport.products()
                    if (reply.code == BillingCode.OK) selectLifetimeOffer(reply.value.orEmpty()) else null
                }
            } catch (_: TimeoutCancellationException) { null }
              catch (e: CancellationException) { publish(BillingStatus.UNAVAILABLE); throw e }
              catch (_: Exception) { null }
            offer = fresh
            if (fresh == null) { publish(BillingStatus.UNAVAILABLE); return@withLock false }
            // If Play changed the price, require another explicit click with that price visible.
            if (fresh.price != displayed.price || fresh.priceMicros != displayed.priceMicros) {
                publish(BillingStatus.READY); return@withLock false
            }
            processingUntil = now() + 120_000
            publish(BillingStatus.PROCESSING)
            val result = try { launch(fresh) } catch (_: Exception) { BillingCode.ERROR }
            if (result != BillingCode.OK) {
                processingUntil = 0
                offer = null
                publish(if (result == BillingCode.CANCELLED) BillingStatus.CANCELLED else BillingStatus.FAILED)
            }
            result == BillingCode.ALREADY_OWNED
        }
        if (alreadyOwned) refresh(restore = true)
    }
    suspend fun callback(code: BillingCode) {
        if (code == BillingCode.OK || code == BillingCode.ALREADY_OWNED) { refresh(purchased = code == BillingCode.OK); return }
        gate.withLock {
            processingUntil = 0
            publish(if (code == BillingCode.CANCELLED) BillingStatus.CANCELLED else BillingStatus.FAILED)
        }
    }
}
