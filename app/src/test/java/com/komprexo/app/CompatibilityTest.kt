package com.komprexo.app

import android.content.pm.ProviderInfo
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompatibilityTest {
    private lateinit var checks: CompatibilityChecks
    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        val provider = GalleryFixtureProvider().apply { streamViaPipe=false;fixtureLoader = GalleryFixtureProvider.FixtureLoader { File("src/test/assets/$it").readBytes() } }
        provider.attachInfo(context,ProviderInfo().apply { authority="com.komprexo.app.test.gallery";exported=true })
        ShadowContentResolver.registerProviderInternal("com.komprexo.app.test.gallery",provider)
        checks=CompatibilityChecks(context) { File("src/test/assets/$it").inputStream() }
    }
    @After fun teardown() { checks.close() }
    @Test fun galleryStreaming() { checks.galleryStreaming() }
    @Test fun providerFailures() { checks.providerFailures() }
    @Test fun invalidUri() { checks.invalidUri() }
    @Test fun jpegTrailer() { checks.jpegTrailer() }
    @Test fun jpegThumbnailCannotHideTruncation() { checks.jpegThumbnailCannotHideTruncation() }
    @Test fun webpDimensions() { checks.webpDimensions() }
    @Test fun decoderFailures() { checks.decoderFailures() }
    @Test fun encoderFailures() { checks.encoderFailures() }
    @Test fun memoryPressure() { checks.memoryPressure() }
    @Test fun errorClassification() { checks.errorClassification() }
    @Test fun diagnosticsPrivacy() { checks.diagnosticsPrivacy() }
}
