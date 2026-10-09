package com.komprexo.app

import com.komprexo.app.access.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.file.Files

class QuotaTest {
    private fun check(block: suspend QuotaChecks.()->Unit)=runBlocking { QuotaChecks().block() }
    @Test fun fiveDailyCredits()=check { fiveCredits() }
    @Test fun sharedSingleBatchPool()=check { sharedPool() }
    @Test fun allFreeBatchLimits()=check { freeBatchLimit() }
    @Test fun premiumTwentyAllWorkflows()=check { premiumLimit() }
    @Test fun freeSingleResizeUnlimited()=check { transforms(Operation.RESIZE,1) }
    @Test fun freeSingleConvertUnlimited()=check { transforms(Operation.CONVERT,1) }
    @Test fun freeBatchResizeNoQuota()=check { transforms(Operation.RESIZE,2) }
    @Test fun freeBatchConvertNoQuota()=check { transforms(Operation.CONVERT,2) }
    @Test fun partialBatchChargesOnlySuccess()=check { partialFailure() }
    @Test fun failedCancelledRelease()=check { failedAndCancelled() }
    @Test fun settlementIdempotent()=check { idempotent() }
    @Test fun reservationsAtomic()=check { reservedCredits() }
    @Test fun concurrentAttemptsCannotBypass()=check { concurrent() }
    @Test fun restartReleasesUncommittedOnly()=check { partialRestart() }
    @Test fun localMidnightReset()=check { midnight() }
    @Test fun rollbackCannotResetQuota()=check { rollback() }
    @Test fun timezoneDoesNotResetTwice()=check { timezone() }
    @Test fun resultCommittedAfterMidnightCountsToday()=check { crossingMidnight() }
    @Test fun datastoreFailureFailsClosed()=check { writeFailure() }
    @Test fun failedSettlementDoesNotCharge()=check { settleFailure() }
    @Test fun malformedLedgerFailsClosed()=check { corruptedBookkeeping() }
    @Test fun unavailableQuotaDoesNotBlockTransforms()=check { invalidLedgerDoesNotBlockSingleTransforms() }
    @Test fun authoritativePresetRules()=check { presetPolicy() }
    @Test fun batchCannotOverReserveRemaining()=check { remainingCreditsLimit() }
    @Test fun failedReleaseRetainsReservationUntilRetry()=check { releaseFailure() }
    @Test fun cancellationCleanupIsNonCancellable()=check { cancellationReleases() }
    @Test fun cancellationDuringDurableReservation()=check { cancellationDuringReservationCommit() }
    @Test fun realDatastoreRestartRecovery()=check { val dir=Files.createTempDirectory("quota").toFile();try { realPersistence(dir) } finally { dir.deleteRecursively() };Unit }
    @Test fun realDatastoreCorruptionNotSilentlyReset()=check { val dir=Files.createTempDirectory("quota").toFile();try { realPersistence(dir,true) } finally { dir.deleteRecursively() };Unit }
}
