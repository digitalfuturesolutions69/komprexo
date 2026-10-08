package com.komprexo.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompatibilityDeviceTest {
    private lateinit var checks: CompatibilityChecks
    @Before fun setup() {
        val i = InstrumentationRegistry.getInstrumentation()
        checks=CompatibilityChecks(i.targetContext) { i.context.assets.open(it) }
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
