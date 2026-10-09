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
    val resizeUsed: Int = 0, val convertUsed: Int = 0,
    val operation: Operation = Operation.COMPRESS,
) {
    fun used(operation: Operation) = when (operation) {
        Operation.COMPRESS -> used
        Operation.RESIZE -> resizeUsed
        Operation.CONVERT -> convertUsed
    }
    fun accepted(operation: Operation) = when (operation) {
        Operation.COMPRESS -> copy(used = used + 1)
        Operation.RESIZE -> copy(resizeUsed = resizeUsed + 1)
        Operation.CONVERT -> copy(convertUsed = convertUsed + 1)
    }
    fun at(clock: Clock): QuotaLedger {
        val now = clock.millis()
        val localDay = LocalDate.now(clock).toEpochDay()
        val reset = localDay > day && now >= highWaterMillis
        return copy(day = if (reset) localDay else day, highWaterMillis = maxOf(now, highWaterMillis), used = if (reset) 0 else used,
            resizeUsed = if (reset) 0 else resizeUsed, convertUsed = if (reset) 0 else convertUsed)
    }
    fun checked(): QuotaLedger {
        require(Operation.entries.all { used(it) in 0..FeatureAccessPolicy.DAILY_FREE })
        require(reserved in 0..FeatureAccessPolicy.DAILY_FREE && used(operation) + reserved <= FeatureAccessPolicy.DAILY_FREE)
        require(settled.size <= FeatureAccessPolicy.DAILY_FREE && (reservation.isNotEmpty() || (reserved == 0 && settled.isEmpty())))
        return this
    }
}
interface QuotaStore { suspend fun update(change: (QuotaLedger) -> QuotaLedger): QuotaLedger }
data class QuotaSnapshot(val remaining: Int? = null, val unavailable: Boolean = false,
    val resizeRemaining: Int? = null, val convertRemaining: Int? = null) {
    fun remaining(operation: Operation) = when(operation) {
        Operation.COMPRESS -> remaining
        Operation.RESIZE -> resizeRemaining
        Operation.CONVERT -> convertRemaining
    }
}
data class QuotaReservation(val id: String, val charged: Boolean, val count: Int, val operation: Operation)

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
            fun remaining(operation: Operation) = FeatureAccessPolicy.DAILY_FREE - ledger.used(operation) -
                if (ledger.operation == operation) ledger.reserved else 0
            mutable.value = QuotaSnapshot(remaining(Operation.COMPRESS), resizeRemaining = remaining(Operation.RESIZE), convertRemaining = remaining(Operation.CONVERT))
            return ledger
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
          catch (_: Exception) { mutable.value = QuotaSnapshot(unavailable = true); throw AccessDenied(Restriction.QUOTA_UNAVAILABLE) }
    }
    private suspend fun initialize() {
        if (!initialized) {
            update { it.copy(reservation = "", reserved = 0, settled = emptySet(), operation = Operation.COMPRESS) }
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
                    val charged = plan == EntitlementState.Free
                    val reservation = QuotaReservation(UUID.randomUUID().toString(), charged, count, operation)
                    // Only verified Premium policy (debug testing for now) bypasses daily counters.
                    if (charged) {
                        initialize()
                        var denied = false
                        update { ledger ->
                            if (ledger.reservation.isNotEmpty() || ledger.used(operation) + count > FeatureAccessPolicy.DAILY_FREE) {
                                denied = true
                                ledger
                            } else ledger.copy(reservation = reservation.id, reserved = count, settled = emptySet(), operation = operation)
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
                check(ledger.operation == reservation.operation)
                ledger.accepted(reservation.operation).copy(reserved = ledger.reserved - 1, settled = ledger.settled + slot.toString())
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
                        if (ledger.reservation == reservation.id) ledger.copy(reservation = "", reserved = 0, settled = emptySet(), operation = Operation.COMPRESS) else ledger
                    }
                } catch(e: AccessDenied) {
                    // Retry journal recovery before future processing; accepted exports remain available.
                    initialized = false
                    throw e
                } finally { active = null; acceptedSlots.clear() }
            }
        }
    }
}
