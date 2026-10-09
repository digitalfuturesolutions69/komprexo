package com.komprexo.app

import com.komprexo.app.access.*
import kotlinx.coroutines.runBlocking
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

/** Same contracts against API 23/26/28/36, including real on-device DataStore I/O. */
class QuotaDeviceTest {
    private fun check(block: suspend QuotaChecks.()->Unit)=runBlocking { QuotaChecks().block() }
    @Test fun cancellationDuringDurableReservation()=check { cancellationDuringReservationCommit() }
    @Test fun sharedFiveCredits()=check { sharedPool();fiveCreditsAfterReset() }
    private suspend fun QuotaChecks.fiveCreditsAfterReset() { midnight();fiveCredits() }
    @Test fun freeAndPremiumAllBatchLimits()=check { freeBatchLimit();premiumLimit() }
    @Test fun transformsRemainUnlimited()=check { transforms(Operation.RESIZE,1);transformsAfterReset() }
    private suspend fun QuotaChecks.transformsAfterReset() { midnight();transforms(Operation.CONVERT,2) }
    @Test fun atomicAndIdempotent()=check { concurrent();idempotent() }
    @Test fun partialCancellationAndRestart()=runBlocking { QuotaChecks().partialFailure();QuotaChecks().partialRestart() }
    @Test fun clockTimezoneAndRollback()=runBlocking { QuotaChecks().rollback();QuotaChecks().timezone() }
    @Test fun realDeviceDataStorePersistsAndRecovers()=check { realPersistence(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir) }
    @Test fun realDeviceCorruptionFailsClosed()=check { realPersistence(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,true) }
    @Test fun storageExceptionsKeepCounts()=runBlocking { QuotaChecks().writeFailure();QuotaChecks().settleFailure() }
    @Test fun corruptionDoesNotBlockTransforms()=check { invalidLedgerDoesNotBlockSingleTransforms() }
}
