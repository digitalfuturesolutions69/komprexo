package com.komprexo.app.access

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
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
    suspend fun consume(count: Int, operation: Operation = Operation.COMPRESS) {
        repeat(count) {
            val r=manager.reserve(operation,1);assertTrue(manager.settle(r,0));manager.release(r)
        }
    }
    suspend fun fiveCredits(operation: Operation = Operation.COMPRESS) { consume(5,operation);assertEquals(0,manager.state.value.remaining(operation));denied(Restriction.DAILY_QUOTA) { manager.reserve(operation,1) } }
    suspend fun sharedPool(operation: Operation = Operation.COMPRESS) { consume(3,operation);val r=manager.reserve(operation,2);assertTrue(manager.settle(r,0));assertTrue(manager.settle(r,1));manager.release(r);assertEquals(0,manager.state.value.remaining(operation)) }
    suspend fun freeBatchLimit() { for(op in Operation.entries) denied(Restriction.BATCH_LIMIT) { manager.reserve(op,3) };assertEquals(0,store.ledger.used) }
    suspend fun premiumLimit() {
        provider.state.value=EntitlementState.Premium
        for(op in Operation.entries) { val r=manager.reserve(op,20);repeat(20) { assertTrue(manager.settle(r,it)) };assertFalse(manager.settle(r,0));manager.release(r);denied(Restriction.BATCH_LIMIT) { manager.reserve(op,21) } }
        assertEquals(0,store.ledger.used)
    }
    suspend fun transforms(op: Operation, count: Int) {
        consume(5)
        var remaining=5
        while(remaining>0) {
            val batchSize=minOf(count,remaining)
            val r=manager.reserve(op,batchSize)
            repeat(batchSize) { assertTrue(manager.settle(r,it)) }
            manager.release(r);remaining-=batchSize
        }
        assertEquals(5,store.ledger.used);assertEquals(5,store.ledger.used(op))
        denied(Restriction.DAILY_QUOTA) { manager.reserve(op,1) }
    }
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
    suspend fun invalidLedgerBlocksAllFreeFeatures() { store.fail=true;for(op in Operation.entries) denied(Restriction.QUOTA_UNAVAILABLE) { manager.reserve(op,1) };assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.resizeUsed);assertEquals(0,store.ledger.convertUsed) }
    suspend fun presetPolicy() { assertEquals(setOf(com.komprexo.app.processing.Preset.DOCUMENT,com.komprexo.app.processing.Preset.MARKETPLACE,com.komprexo.app.processing.Preset.CUSTOM),FeatureAccessPolicy.basicPresets);for(p in FeatureAccessPolicy.premiumPresets) denied(Restriction.PREMIUM_PRESET) { manager.reserve(Operation.COMPRESS,1,p) };provider.state.value=EntitlementState.Premium;for(p in com.komprexo.app.processing.Preset.entries) { val r=manager.reserve(Operation.COMPRESS,1,p);manager.release(r) } }
    suspend fun remainingCreditsLimit() { consume(4);denied(Restriction.DAILY_QUOTA) { manager.reserve(Operation.COMPRESS,2) };assertEquals(4,store.ledger.used);assertEquals(0,store.ledger.reserved) }
    suspend fun releaseFailure() { val r=manager.reserve(Operation.COMPRESS,1);store.fail=true;denied(Restriction.QUOTA_UNAVAILABLE) { manager.release(r) };denied(Restriction.QUOTA_UNAVAILABLE) { manager.reserve(Operation.COMPRESS,1) };denied(Restriction.QUOTA_UNAVAILABLE) { manager.reserve(Operation.RESIZE,1) };store.fail=false;consume(5) }
    suspend fun cancellationReleases() { val r=manager.reserve(Operation.COMPRESS,2);val job=CoroutineScope(Dispatchers.Default).launch { try { awaitCancellation() } finally { manager.release(r) } };yield();job.cancelAndJoin(); // If cancelled before launch started, caller still releases its reservation.
        manager.release(r);assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.reserved) }
    suspend fun cancellationDuringReservationCommit(operation: Operation = Operation.COMPRESS) = coroutineScope {
        val written=CompletableDeferred<Unit>();val resume=CompletableDeferred<Unit>()
        val durable=object : QuotaStore {
            override suspend fun update(change: (QuotaLedger)->QuotaLedger): QuotaLedger {
                val next=store.update(change)
                if(next.reservation.isNotEmpty()) { written.complete(Unit);resume.await() }
                return next
            }
        }
        val m=DailyQuotaManager(durable,provider) { clock }
        val attempt=launch(Dispatchers.Default) { val r=m.reserve(operation,1);m.release(r) }
        written.await();attempt.cancel();resume.complete(Unit);attempt.join()
        assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.reserved)
        m.refresh();assertEquals(5,m.state.value.remaining(operation))
    }
    suspend fun independentCounters() {
        for(op in Operation.entries) {
            consume(5,op)
            assertEquals(5,store.ledger.used(op))
        }
        assertEquals(listOf(5,5,5),Operation.entries.map { store.ledger.used(it) })
        for(op in Operation.entries) denied(Restriction.DAILY_QUOTA) { manager.reserve(op,1) }
    }
    suspend fun featureLifecycle(op: Operation) {
        val r=manager.reserve(op,2)
        manager.settle(r,0);assertFalse(manager.settle(r,0))
        manager.release(r);assertEquals(1,store.ledger.used(op))
        assertEquals("",store.ledger.reservation);assertTrue(store.ledger.settled.isEmpty());assertEquals(Operation.COMPRESS,store.ledger.operation)
        val cancelled=manager.reserve(op,2);manager.release(cancelled);assertEquals(1,store.ledger.used(op))
        val interrupted=manager.reserve(op,2);manager.settle(interrupted,0)
        val recovered=DailyQuotaManager(store,provider) { clock };recovered.refresh()
        assertEquals(2,store.ledger.used(op));assertEquals(3,recovered.state.value.remaining(op));assertEquals(0,store.ledger.reserved)
        clock=Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"),ZoneId.of("Asia/Jakarta"));recovered.refresh();assertEquals(3,recovered.state.value.remaining(op))
        clock=Clock.fixed(Instant.parse("2026-10-09T17:00:00Z"),ZoneId.of("Asia/Jakarta"));recovered.refresh();assertEquals(5,recovered.state.value.remaining(op))
        assertTrue(Operation.entries.all { store.ledger.used(it)==0 })
    }
    suspend fun featureTimezone(op: Operation) {
        consume(5,op);clock=clock.withZone(ZoneId.of("Pacific/Kiritimati"));manager.refresh();assertEquals(5,manager.state.value.remaining(op))
        consume(1,op);clock=clock.withZone(ZoneId.of("America/Los_Angeles"));manager.refresh();assertEquals(4,manager.state.value.remaining(op))
        clock=clock.withZone(ZoneId.of("Pacific/Kiritimati"));manager.refresh();assertEquals(4,manager.state.value.remaining(op))
    }
    suspend fun featureConcurrent(op: Operation) = coroutineScope {
        val attempts=List(20) { async(Dispatchers.Default) { try { manager.reserve(op,2) } catch(_: AccessDenied) { null } } }.awaitAll()
        val r=attempts.filterNotNull().single();assertEquals(op,store.ledger.operation)
        assertEquals(2,store.ledger.reserved);manager.settle(r,0);manager.release(r);assertEquals(1,store.ledger.used(op))
    }
    suspend fun featureMidnightCommit(op: Operation) {
        val r=manager.reserve(op,2);manager.settle(r,0)
        clock=Clock.fixed(Instant.parse("2026-10-09T17:00:00Z"),ZoneId.of("Asia/Jakarta"));manager.refresh()
        assertFalse(manager.settle(r,0));manager.settle(r,1);manager.release(r);assertEquals(1,store.ledger.used(op))
    }
    suspend fun allCountersMidnight() {
        independentCounters()
        clock=Clock.fixed(Instant.parse("2026-10-09T17:00:00Z"),ZoneId.of("Asia/Jakarta"));manager.refresh()
        for(op in Operation.entries) { assertEquals(0,store.ledger.used(op));assertEquals(5,manager.state.value.remaining(op)) }
    }
    suspend fun mixedFeatureConcurrent() = coroutineScope {
        val attempts=List(20) { index -> async(Dispatchers.Default) {
            try { manager.reserve(Operation.entries[index%3],2) } catch(_: AccessDenied) { null }
        } }.awaitAll()
        val winner=attempts.filterNotNull().single();manager.settle(winner,0);manager.release(winner)
        for(op in Operation.entries) assertEquals(if(op==winner.operation) 1 else 0,store.ledger.used(op))
    }
    suspend fun legacyMigration(directory: File) {
        val file=File(directory,"legacy-${UUID.randomUUID()}.preferences_pb")
        var job=SupervisorJob()
        try {
            val ds=PreferenceDataStoreFactory.create(scope=CoroutineScope(job+Dispatchers.IO),produceFile={file})
            ds.edit { p ->
                p[androidx.datastore.preferences.core.intPreferencesKey("used")]=4
                p[androidx.datastore.preferences.core.longPreferencesKey("day")]=java.time.LocalDate.now(clock).toEpochDay()
                p[androidx.datastore.preferences.core.longPreferencesKey("clock_high_water")]=clock.millis()
            }
            job.cancelAndJoin();job=SupervisorJob()
            val upgraded=PreferenceDataStoreFactory.create(scope=CoroutineScope(job+Dispatchers.IO),produceFile={file})
            val m=DailyQuotaManager(PreferencesQuotaStore(upgraded),provider) { clock };m.refresh()
            assertEquals(1,m.state.value.remaining);assertEquals(5,m.state.value.resizeRemaining);assertEquals(5,m.state.value.convertRemaining)
            val r=m.reserve(Operation.RESIZE,1);m.settle(r,0);m.release(r)
            assertEquals(1,m.state.value.remaining);assertEquals(4,m.state.value.resizeRemaining);assertEquals(5,m.state.value.convertRemaining)
        } finally { job.cancelAndJoin();file.delete() }
    }
    suspend fun realPersistence(directory: File, corrupt: Boolean=false, operation: Operation = Operation.COMPRESS) {
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
                open { m->val r=m.reserve(operation,2);m.settle(r,0) /* crash before releasing remaining slot */ }
                open { m->m.refresh();assertEquals(4,m.state.value.remaining(operation));val r=m.reserve(operation,1);m.settle(r,0);m.release(r) }
                open { m->m.refresh();assertEquals(3,m.state.value.remaining(operation)) }
            }
        } finally { file.delete() }
    }
}

/** Every independent quota scenario owns a fresh journal; verify these compositions on JVM too. */
suspend fun sharedFiveCreditsChecks() {
    QuotaChecks().sharedPool()
    QuotaChecks().fiveCredits()
    QuotaChecks().midnight()
}
suspend fun boundedTransformChecks() {
    for(operation in listOf(Operation.RESIZE,Operation.CONVERT)) {
        for(count in 1..2) QuotaChecks().transforms(operation,count)
    }
}
