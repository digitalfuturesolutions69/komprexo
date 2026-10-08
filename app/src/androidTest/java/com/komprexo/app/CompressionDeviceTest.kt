package com.komprexo.app
import org.junit.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
@RunWith(AndroidJUnit4::class)
class CompressionDeviceTest {
    private lateinit var checks: EngineChecks
    @Before fun setup() { val i=InstrumentationRegistry.getInstrumentation();checks=EngineChecks(i.targetContext) { i.context.assets.open(it) } }
    @After fun teardown() { checks.close() }
    @Test fun jpeg() { checks.jpeg() }
    @Test fun png() { checks.png() }
    @Test fun webp() { checks.webp() }
    @Test fun highestQuality() { checks.highestQuality() }
    @Test fun presets() { checks.presets() }
    @Test fun customTarget() { checks.customTarget() }
    @Test fun aspectRatio() { checks.aspectRatio() }
    @Test fun exifRotation() { checks.exifRotation() }
    @Test fun allOrientations() { checks.allOrientations() }
    @Test fun transparency() { checks.transparency() }
    @Test fun jpegConversion() { checks.jpegConversion() }
    @Test fun largeInput() { checks.largeInput() }
    @Test fun samplingBoundaries() { checks.samplingBoundaries() }
    @Test fun smallSource() { checks.smallSource() }
    @Test fun corruption() { checks.corruption() }
    @Test fun unsupported() { checks.unsupported() }
    @Test fun oversizeBytes() { checks.oversizeBytes() }
    @Test fun unreachable() { checks.unreachable() }
    @Test fun cancellation() { checks.cancellation() }
    @Test fun outputIntegrity() { checks.outputIntegrity() }
    @Test fun storageFailure() { checks.storageFailure() }
    @Test fun concurrency() { checks.concurrency() }
    @Test fun saveAndImport() { checks.saveAndImport() }
    @Test fun revokedAccess() { checks.revokedAccess() }
    @Test fun shareSecurity() { checks.shareSecurity() }
}
