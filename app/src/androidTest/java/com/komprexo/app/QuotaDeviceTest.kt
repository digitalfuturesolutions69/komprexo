package com.komprexo.app

import com.komprexo.app.access.*
import kotlinx.coroutines.runBlocking
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

/** Same contracts against API 23/26/28/36, including real on-device DataStore I/O. */
class QuotaDeviceTest {
    private fun check(block: suspend QuotaChecks.()->Unit)=runBlocking { QuotaChecks().block() }
    @Test fun cancellationDuringDurableReservation()=check { cancellationDuringReservationCommit() }
    @Test fun sharedFiveCredits()=runBlocking { sharedFiveCreditsChecks() }
    @Test fun freeAndPremiumAllBatchLimits()=check { freeBatchLimit();premiumLimit() }
    @Test fun transformsHaveIndependentQuotas()=runBlocking { boundedTransformChecks() }
    @Test fun atomicAndIdempotent()=check { concurrent();idempotent() }
    @Test fun partialCancellationAndRestart()=runBlocking { QuotaChecks().partialFailure();QuotaChecks().partialRestart() }
    @Test fun clockTimezoneAndRollback()=runBlocking { QuotaChecks().rollback();QuotaChecks().timezone() }
    @Test fun allThreeCountersAndSharedPools()=runBlocking {
        QuotaChecks().independentCounters();QuotaChecks().allCountersMidnight();QuotaChecks().mixedFeatureConcurrent()
        for(op in Operation.entries) QuotaChecks().sharedPool(op)
    }
    @Test fun allFeatureLifecycleAndClockContracts()=runBlocking {
        for(op in Operation.entries) {
            QuotaChecks().featureLifecycle(op);QuotaChecks().featureTimezone(op)
            QuotaChecks().featureConcurrent(op);QuotaChecks().featureMidnightCommit(op);QuotaChecks().cancellationDuringReservationCommit(op)
        }
    }
    @Test fun allFeatureRealDevicePersistence()=runBlocking {
        for(op in Operation.entries) QuotaChecks().realPersistence(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,operation=op)
        QuotaChecks().legacyMigration(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)
    }
    @Test fun realDeviceDataStorePersistsAndRecovers()=check { realPersistence(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir) }
    @Test fun realDeviceCorruptionFailsClosed()=check { realPersistence(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,true) }
    @Test fun storageExceptionsKeepCounts()=runBlocking { QuotaChecks().writeFailure();QuotaChecks().settleFailure() }
    @Test fun corruptionBlocksAllFreeProcessing()=check { invalidLedgerBlocksAllFreeFeatures() }
}
