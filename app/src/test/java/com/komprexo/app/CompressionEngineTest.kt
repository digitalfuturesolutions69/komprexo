package com.komprexo.app
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompressionEngineTest {
    private lateinit var checks: EngineChecks
    @Before fun setup() {
        // Robolectric gives each test a fresh cache directory; AndroidX's static
        // provider strategy cache must follow that context, as it does per process.
        val field=androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
        field.isAccessible=true
        (field.get(null) as MutableMap<*, *>).clear()
        checks=EngineChecks(RuntimeEnvironment.getApplication()) { File("src/test/assets/$it").inputStream() } }
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
