package com.komprexo.app.access

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class MemoryQuotaStore : QuotaStore {
    var ledger=QuotaLedger()
    var fail=false
    private val mutex=Mutex()
    override suspend fun update(change: (QuotaLedger)->QuotaLedger): QuotaLedger = mutex.withLock {
        if(fail) throw java.io.IOException("test storage failure")
        change(ledger).also { ledger=it }
    }
}
class TestEntitlements : EntitlementProvider { override val state=MutableStateFlow<EntitlementState>(EntitlementState.Free) }
class QuotaChecks {
    val store=MemoryQuotaStore()
    val provider=TestEntitlements()
    var clock: Clock=Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"),ZoneId.of("Asia/Jakarta"))
    val manager=DailyQuotaManager(store,provider) { clock }
    suspend fun denied(reason: Restriction, action: suspend ()->Unit) {
        try { action();fail("Expected $reason") } catch(e: AccessDenied) { assertEquals(reason,e.reason) }
    }
    suspend fun consume(count: Int) {
        repeat(count) {
            val r=manager.reserve(Operation.COMPRESS,1);assertTrue(manager.settle(r,0));manager.release(r)
        }
    }
    suspend fun fiveCredits() { consume(5);assertEquals(0,manager.state.value.remaining);denied(Restriction.DAILY_QUOTA) { manager.reserve(Operation.COMPRESS,1) } }
    suspend fun sharedPool() { consume(3);val r=manager.reserve(Operation.COMPRESS,2);assertTrue(manager.settle(r,0));assertTrue(manager.settle(r,1));manager.release(r);assertEquals(0,manager.state.value.remaining) }
    suspend fun freeBatchLimit() { for(op in Operation.entries) denied(Restriction.BATCH_LIMIT) { manager.reserve(op,3) };assertEquals(0,store.ledger.used) }
    suspend fun premiumLimit() {
        provider.state.value=EntitlementState.Premium
        for(op in Operation.entries) { val r=manager.reserve(op,20);repeat(20) { assertTrue(manager.settle(r,it)) };assertFalse(manager.settle(r,0));manager.release(r);denied(Restriction.BATCH_LIMIT) { manager.reserve(op,21) } }
        assertEquals(0,store.ledger.used)
    }
    suspend fun transforms(op: Operation, count: Int) { consume(5);repeat(7) { val r=manager.reserve(op,count);manager.release(r) };assertEquals(5,store.ledger.used) }
    suspend fun partialFailure() { val r=manager.reserve(Operation.COMPRESS,2);manager.settle(r,0);manager.release(r);assertEquals(1,store.ledger.used);assertEquals(4,manager.state.value.remaining) }
    suspend fun failedAndCancelled() { val r=manager.reserve(Operation.COMPRESS,2);manager.release(r);assertEquals(0,store.ledger.used);assertEquals(5,manager.state.value.remaining) }
    suspend fun idempotent() { val r=manager.reserve(Operation.COMPRESS,2);assertTrue(manager.settle(r,0));assertFalse(manager.settle(r,0));manager.release(r);assertFalse(manager.settle(r,0));assertEquals(1,store.ledger.used) }
    suspend fun reservedCredits() { consume(3);val r=manager.reserve(Operation.COMPRESS,2);assertEquals(0,manager.state.value.remaining);denied(Restriction.BUSY) { manager.reserve(Operation.COMPRESS,1) };manager.release(r);assertEquals(2,manager.state.value.remaining) }
    suspend fun concurrent() = coroutineScope {
        val attempts=List(20) { async(Dispatchers.Default) { try { manager.reserve(Operation.COMPRESS,2) } catch(_: AccessDenied) { null } } }.awaitAll()
        assertEquals(1,attempts.count { it!=null });assertEquals(2,store.ledger.reserved);manager.release(attempts.filterNotNull().single())
    }
    suspend fun partialRestart() { val r=manager.reserve(Operation.COMPRESS,2);manager.settle(r,0);val recovered=DailyQuotaManager(store,provider) { clock };recovered.refresh();assertEquals(1,store.ledger.used);assertEquals(0,store.ledger.reserved);assertEquals(4,recovered.state.value.remaining) }
    suspend fun midnight() { consume(5);clock=Clock.fixed(Instant.parse("2026-10-09T17:00:00Z"),ZoneId.of("Asia/Jakarta"));manager.refresh();assertEquals(5,manager.state.value.remaining) }
    suspend fun rollback() { consume(5);clock=Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"),ZoneId.of("Asia/Jakarta"));manager.refresh();assertEquals(0,manager.state.value.remaining);clock=Clock.fixed(Instant.parse("2026-10-09T11:00:00Z"),ZoneId.of("Asia/Jakarta"));manager.refresh();assertEquals(0,manager.state.value.remaining) }
    suspend fun timezone() { consume(5);clock=clock.withZone(ZoneId.of("Pacific/Kiritimati"));manager.refresh();assertEquals(5,manager.state.value.remaining);consume(1);clock=clock.withZone(ZoneId.of("America/Los_Angeles"));manager.refresh();assertEquals(4,manager.state.value.remaining);clock=clock.withZone(ZoneId.of("Pacific/Kiritimati"));manager.refresh();assertEquals(4,manager.state.value.remaining) }
    suspend fun crossingMidnight() { val r=manager.reserve(Operation.COMPRESS,2);manager.settle(r,0);clock=Clock.fixed(Instant.parse("2026-10-09T17:00:00Z"),ZoneId.of("Asia/Jakarta"));manager.refresh();assertFalse(manager.settle(r,0));assertTrue(manager.settle(r,1));manager.release(r);assertEquals(1,store.ledger.used) }
    suspend fun writeFailure() { store.fail=true;denied(Restriction.QUOTA_UNAVAILABLE) { manager.reserve(Operation.COMPRESS,1) };assertTrue(manager.state.value.unavailable);store.fail=false;consume(1);assertEquals(1,store.ledger.used) }
    suspend fun settleFailure() { val r=manager.reserve(Operation.COMPRESS,1);store.fail=true;denied(Restriction.QUOTA_UNAVAILABLE) { manager.settle(r,0) };assertEquals(0,store.ledger.used);store.fail=false;manager.release(r);assertEquals(5,manager.state.value.remaining) }
    suspend fun corruptedBookkeeping() { store.ledger=QuotaLedger(used=-1);denied(Restriction.QUOTA_UNAVAILABLE) { manager.reserve(Operation.COMPRESS,1) };assertEquals(-1,store.ledger.used) }
    suspend fun invalidLedgerDoesNotBlockSingleTransforms() { store.fail=true;for(op in listOf(Operation.RESIZE,Operation.CONVERT)) { val r=manager.reserve(op,1);manager.release(r) };assertEquals(0,store.ledger.used) }
    suspend fun presetPolicy() { assertEquals(setOf(com.komprexo.app.processing.Preset.DOCUMENT,com.komprexo.app.processing.Preset.MARKETPLACE,com.komprexo.app.processing.Preset.CUSTOM),FeatureAccessPolicy.basicPresets);for(p in FeatureAccessPolicy.premiumPresets) denied(Restriction.PREMIUM_PRESET) { manager.reserve(Operation.COMPRESS,1,p) };provider.state.value=EntitlementState.Premium;for(p in com.komprexo.app.processing.Preset.entries) { val r=manager.reserve(Operation.COMPRESS,1,p);manager.release(r) } }
    suspend fun remainingCreditsLimit() { consume(4);denied(Restriction.DAILY_QUOTA) { manager.reserve(Operation.COMPRESS,2) };assertEquals(4,store.ledger.used);assertEquals(0,store.ledger.reserved) }
    suspend fun releaseFailure() { val r=manager.reserve(Operation.COMPRESS,1);store.fail=true;denied(Restriction.QUOTA_UNAVAILABLE) { manager.release(r) };denied(Restriction.QUOTA_UNAVAILABLE) { manager.reserve(Operation.COMPRESS,1) };val transform=manager.reserve(Operation.RESIZE,1);manager.release(transform);store.fail=false;consume(5) }
    suspend fun cancellationReleases() { val r=manager.reserve(Operation.COMPRESS,2);val job=CoroutineScope(Dispatchers.Default).launch { try { awaitCancellation() } finally { manager.release(r) } };yield();job.cancelAndJoin(); // If cancelled before launch started, caller still releases its reservation.
        manager.release(r);assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.reserved) }
    suspend fun cancellationDuringReservationCommit() = coroutineScope {
        val written=CompletableDeferred<Unit>();val resume=CompletableDeferred<Unit>()
        val durable=object : QuotaStore {
            override suspend fun update(change: (QuotaLedger)->QuotaLedger): QuotaLedger {
                val next=store.update(change)
                if(next.reservation.isNotEmpty()) { written.complete(Unit);resume.await() }
                return next
            }
        }
        val m=DailyQuotaManager(durable,provider) { clock }
        val attempt=launch(Dispatchers.Default) { val r=m.reserve(Operation.COMPRESS,1);m.release(r) }
        written.await();attempt.cancel();resume.complete(Unit);attempt.join()
        assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.reserved)
        m.refresh();assertEquals(5,m.state.value.remaining)
    }
    suspend fun realPersistence(directory: File, corrupt: Boolean=false) {
        val file=File(directory,"quota-${UUID.randomUUID()}.preferences_pb")
        suspend fun open(action: suspend (DailyQuotaManager)->Unit) {
            val job=SupervisorJob();val scope=CoroutineScope(job+Dispatchers.IO)
            val ds=PreferenceDataStoreFactory.create(scope=scope,produceFile={ file })
            val m=DailyQuotaManager(PreferencesQuotaStore(ds),provider) { clock }
            try { action(m) } finally { job.cancelAndJoin() }
        }
        try {
            if(corrupt) {
                file.writeBytes(byteArrayOf(0,1,2,3,4))
                open { m->denied(Restriction.QUOTA_UNAVAILABLE) { m.reserve(Operation.COMPRESS,1) };assertTrue(m.state.value.unavailable) }
                assertArrayEquals(byteArrayOf(0,1,2,3,4),file.readBytes())
            } else {
                open { m->val r=m.reserve(Operation.COMPRESS,2);m.settle(r,0) /* crash before releasing remaining slot */ }
                open { m->m.refresh();assertEquals(4,m.state.value.remaining);val r=m.reserve(Operation.COMPRESS,1);m.settle(r,0);m.release(r) }
                open { m->m.refresh();assertEquals(3,m.state.value.remaining) }
            }
        } finally { file.delete() }
    }
}
