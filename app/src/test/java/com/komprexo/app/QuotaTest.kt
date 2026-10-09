package com.komprexo.app

import com.komprexo.app.access.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.file.Files

class QuotaTest {
    private fun check(block: suspend QuotaChecks.()->Unit)=runBlocking { QuotaChecks().block() }
    @Test fun deviceDailyScenarioComposition()=runBlocking { sharedFiveCreditsChecks() }
    @Test fun deviceTransformScenarioComposition()=runBlocking { boundedTransformChecks() }
    @Test fun fiveDailyCredits()=check { fiveCredits() }
    @Test fun sharedSingleBatchPool()=check { sharedPool() }
    @Test fun allFreeBatchLimits()=check { freeBatchLimit() }
    @Test fun premiumTwentyAllWorkflows()=check { premiumLimit() }
    @Test fun freeSingleResizeFiveCredits()=check { transforms(Operation.RESIZE,1) }
    @Test fun freeSingleConvertFiveCredits()=check { transforms(Operation.CONVERT,1) }
    @Test fun freeBatchResizeSharesQuota()=check { transforms(Operation.RESIZE,2) }
    @Test fun freeBatchConvertSharesQuota()=check { transforms(Operation.CONVERT,2) }
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
    @Test fun unavailableQuotaBlocksAllFreeProcessing()=check { invalidLedgerBlocksAllFreeFeatures() }
    @Test fun authoritativePresetRules()=check { presetPolicy() }
    @Test fun batchCannotOverReserveRemaining()=check { remainingCreditsLimit() }
    @Test fun failedReleaseRetainsReservationUntilRetry()=check { releaseFailure() }
    @Test fun cancellationCleanupIsNonCancellable()=check { cancellationReleases() }
    @Test fun cancellationDuringDurableReservation()=check { cancellationDuringReservationCommit() }
    @Test fun threeCountersIndependent()=check { independentCounters() }
    @Test fun resizeSingleBatchPool()=check { sharedPool(Operation.RESIZE) }
    @Test fun convertSingleBatchPool()=check { sharedPool(Operation.CONVERT) }
    @Test fun resizeLifecycleAndClockRollback()=check { featureLifecycle(Operation.RESIZE) }
    @Test fun convertLifecycleAndClockRollback()=check { featureLifecycle(Operation.CONVERT) }
    @Test fun resizeTimezoneNoDoubleReset()=check { featureTimezone(Operation.RESIZE) }
    @Test fun convertTimezoneNoDoubleReset()=check { featureTimezone(Operation.CONVERT) }
    @Test fun resizeConcurrentReservation()=check { featureConcurrent(Operation.RESIZE) }
    @Test fun convertConcurrentReservation()=check { featureConcurrent(Operation.CONVERT) }
    @Test fun resizeCommitAcrossMidnight()=check { featureMidnightCommit(Operation.RESIZE) }
    @Test fun convertCommitAcrossMidnight()=check { featureMidnightCommit(Operation.CONVERT) }
    @Test fun resizeCancellationDuringDurableReservation()=check { cancellationDuringReservationCommit(Operation.RESIZE) }
    @Test fun convertCancellationDuringDurableReservation()=check { cancellationDuringReservationCommit(Operation.CONVERT) }
    @Test fun allCountersResetAtomicallyAtMidnight()=check { allCountersMidnight() }
    @Test fun concurrentMixedFeaturesChargeWinnerOnly()=check { mixedFeatureConcurrent() }
    @Test fun legacyCompressionCounterMigration()=check {
        val dir=Files.createTempDirectory("legacy").toFile()
        try { legacyMigration(dir) } finally { dir.deleteRecursively() };Unit
    }
    @Test fun allCountersRealDiskRecovery()=check {
        val dir=Files.createTempDirectory("quotas").toFile()
        try { for(op in Operation.entries) realPersistence(dir,operation=op) } finally { dir.deleteRecursively() };Unit
    }
    @Test fun realDatastoreRestartRecovery()=check { val dir=Files.createTempDirectory("quota").toFile();try { realPersistence(dir) } finally { dir.deleteRecursively() };Unit }
    @Test fun realDatastoreCorruptionNotSilentlyReset()=check { val dir=Files.createTempDirectory("quota").toFile();try { realPersistence(dir,true) } finally { dir.deleteRecursively() };Unit }
}
