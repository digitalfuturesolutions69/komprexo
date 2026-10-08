package com.komprexo.app
import android.content.pm.ProviderInfo
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase2Test {
    private lateinit var checks: Phase2Checks
    @Before fun setup() {
        val context=RuntimeEnvironment.getApplication()
        val provider=DocumentsFixtureProvider()
        provider.attachInfo(context,ProviderInfo().apply { authority=DocumentsFixtureProvider.AUTHORITY;exported=true })
        ShadowContentResolver.registerProviderInternal(DocumentsFixtureProvider.AUTHORITY,provider)
        val field=androidx.core.content.FileProvider::class.java.getDeclaredField("sCache");field.isAccessible=true
        (field.get(null) as MutableMap<*,*>).clear()
        checks=Phase2Checks(context) { File("src/test/assets/$it").inputStream() }
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
    @Test fun shareSafetyLimits() { checks.shareSafetyLimits() }
    @Test fun measuredBatch() { checks.measuredBatch() }
}
