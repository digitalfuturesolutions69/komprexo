package com.komprexo.app.access

import com.komprexo.app.processing.Preset
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** Only aggregate counts and the current reservation journal; never image identifiers. */
data class QuotaLedger(
    val day: Long = Long.MIN_VALUE, val highWaterMillis: Long = Long.MIN_VALUE,
    val used: Int = 0, val reservation: String = "", val reserved: Int = 0,
    val settled: Set<String> = emptySet(),
) {
    fun at(clock: Clock): QuotaLedger {
        val now = clock.millis()
        val localDay = LocalDate.now(clock).toEpochDay()
        val reset = localDay > day && now >= highWaterMillis
        return copy(day = if (reset) localDay else day, highWaterMillis = maxOf(now, highWaterMillis), used = if (reset) 0 else used)
    }
    fun checked(): QuotaLedger {
        require(used in 0..FeatureAccessPolicy.DAILY_FREE && reserved in 0..FeatureAccessPolicy.DAILY_FREE && used + reserved <= FeatureAccessPolicy.DAILY_FREE)
        require(settled.size <= FeatureAccessPolicy.DAILY_FREE && (reservation.isNotEmpty() || (reserved == 0 && settled.isEmpty())))
        return this
    }
}
interface QuotaStore { suspend fun update(change: (QuotaLedger) -> QuotaLedger): QuotaLedger }
data class QuotaSnapshot(val remaining: Int? = null, val unavailable: Boolean = false)
data class QuotaReservation(val id: String, val charged: Boolean, val count: Int)

/** One process-wide instance. Reservations prevent concurrent work across all screens.
 * Durable settlement precedes output publication. Startup releases uncommitted reservations. */
class DailyQuotaManager(private val store: QuotaStore, private val provider: EntitlementProvider,
    private val clock: () -> Clock = { Clock.systemDefaultZone() }) {
    private val mutex = Mutex()
    private var initialized = false
    private var active: QuotaReservation? = null
    private val acceptedSlots = mutableSetOf<Int>()
    private val mutable = MutableStateFlow(QuotaSnapshot())
    val state = mutable.asStateFlow()
    val entitlement = provider.state

    private suspend fun update(change: (QuotaLedger) -> QuotaLedger): QuotaLedger {
        try {
            val ledger = store.update { old -> change(old.checked().at(clock())).checked() }
            mutable.value = QuotaSnapshot((FeatureAccessPolicy.DAILY_FREE - ledger.used - ledger.reserved).coerceAtLeast(0))
            return ledger
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
          catch (_: Exception) { mutable.value = QuotaSnapshot(unavailable = true); throw AccessDenied(Restriction.QUOTA_UNAVAILABLE) }
    }
    private suspend fun initialize() {
        if (!initialized) {
            update { it.copy(reservation = "", reserved = 0, settled = emptySet()) }
            initialized = true
        }
    }
    suspend fun refresh() = mutex.withLock { initialize(); update { it }; Unit }

    suspend fun reserve(operation: Operation, count: Int, preset: Preset = Preset.CUSTOM): QuotaReservation {
        var acquired: QuotaReservation? = null
        try {
            return withContext(NonCancellable) {
                mutex.withLock {
                    if (active != null) throw AccessDenied(Restriction.BUSY)
                    val plan = entitlement.value
                    FeatureAccessPolicy.check(plan, count, preset)
                    val charged = operation == Operation.COMPRESS && plan == EntitlementState.Free
                    val reservation = QuotaReservation(UUID.randomUUID().toString(), charged, count)
                    // Unlimited transforms/Premium do not require a readable compression ledger.
                    if (charged) {
                        initialize()
                        var denied = false
                        update { ledger ->
                            if (ledger.reservation.isNotEmpty() || ledger.used + count > FeatureAccessPolicy.DAILY_FREE) {
                                denied = true
                                ledger
                            } else ledger.copy(reservation = reservation.id, reserved = count, settled = emptySet())
                        }
                        if (denied) throw AccessDenied(Restriction.DAILY_QUOTA)
                    }
                    active = reservation
                    acceptedSlots.clear()
                    acquired = reservation
                    reservation
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // edit may commit while cancellation is delivered at the dispatcher boundary.
            acquired?.let { release(it) }
            throw e
        }
    }
    /** Slot is an operation-local integer, never a URI. Duplicate settlement is a no-op. */
    suspend fun settle(reservation: QuotaReservation, slot: Int): Boolean = mutex.withLock {
        require(slot in 0 until reservation.count)
        if (active != reservation) return@withLock false
        if (slot in acceptedSlots) return@withLock false
        if (!reservation.charged) { acceptedSlots.add(slot); return@withLock true }
        var accepted = false
        update { ledger ->
            if (ledger.reservation != reservation.id || slot.toString() in ledger.settled) ledger
            else {
                check(ledger.reserved > 0)
                accepted = true
                ledger.copy(used = ledger.used + 1, reserved = ledger.reserved - 1, settled = ledger.settled + slot.toString())
            }
        }
        if (accepted) acceptedSlots.add(slot)
        accepted
    }
    suspend fun release(reservation: QuotaReservation) = withContext(NonCancellable) {
        mutex.withLock {
            if (active == reservation) {
                try {
                    if (reservation.charged) update { ledger ->
                        if (ledger.reservation == reservation.id) ledger.copy(reservation = "", reserved = 0, settled = emptySet()) else ledger
                    }
                } catch(e: AccessDenied) {
                    // Retry journal recovery before any future charge; transforms remain available.
                    initialized = false
                    throw e
                } finally { active = null; acceptedSlots.clear() }
            }
        }
    }
}
