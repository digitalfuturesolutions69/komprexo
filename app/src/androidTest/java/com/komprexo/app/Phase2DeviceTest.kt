package com.komprexo.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class Phase2DeviceTest {
    private lateinit var checks: Phase2Checks
    @Before fun setup() {
        val i=InstrumentationRegistry.getInstrumentation()
        checks=Phase2Checks(i.targetContext) { i.context.assets.open(it) }
    }
    @After fun teardown() { checks.close() }

    @Test fun jpegPng() { checks.jpegPng() }
    @Test fun pngJpeg() { checks.pngJpeg() }
    @Test fun jpegWebp() { checks.jpegWebp() }
    @Test fun webpJpeg() { checks.webpJpeg() }
    @Test fun pngTransparency() { checks.pngTransparency() }
    @Test fun unsupportedAndCorrupt() { checks.unsupportedAndCorrupt() }
    @Test fun percentage() { checks.percentage() }
    @Test fun fixedDimensions() { checks.fixedDimensions() }
    @Test fun aspectRatio() { checks.aspectRatio() }
    @Test fun invalidDimensions() { checks.invalidDimensions() }
    @Test fun orientation() { checks.orientation() }
    @Test fun memoryAndOversize() { checks.memoryAndOversize() }
    @Test fun presets() { checks.presets() }
    @Test fun batchSequential() { checks.batchSequential() }
    @Test fun partialBatch() { checks.partialBatch() }
    @Test fun batchCancellation() { checks.batchCancellation() }
    @Test fun batchLimitsAndConcurrency() { checks.batchLimitsAndConcurrency() }
    @Test fun nativeGate() { checks.nativeGate() }
    @Test fun transformCancellation() { checks.transformCancellation() }
    @Test fun multiShare() { checks.multiShare() }
    @Test fun folderExport() { checks.folderExport() }
    @Test fun partialExportAndPermissions() { checks.partialExportAndPermissions() }
    @Test fun exportStorageAndCancellation() { checks.exportStorageAndCancellation() }
    @Test fun converterEncoderAndOutputFailures() { checks.converterEncoderAndOutputFailures() }
    @Test fun shareSafetyLimits() { checks.shareSafetyLimits() }
    @Test fun measuredBatch() { checks.measuredBatch() }
}
