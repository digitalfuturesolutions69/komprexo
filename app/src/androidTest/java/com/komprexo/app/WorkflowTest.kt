package com.komprexo.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import com.komprexo.app.ui.CompressionViewModel
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class WorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun openAcceptedCompressionWorkflow() { compose.onNodeWithTag("homecompress").performClick() }
    @Test fun pickerCompressionPreviewSaveAndShare() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val shared=File(context.cacheDir,"shared").apply { mkdirs() }
        val source=File(shared,"fixture-${java.util.UUID.randomUUID()}.jpg")
        instrumentation.context.assets.open("noise.jpg").use { input -> source.outputStream().use { input.copyTo(it) } }
        val destination=File(shared,"saved-${java.util.UUID.randomUUID()}.jpg").apply { createNewFile() }
        val authority="${context.packageName}.files"
        val inputUri=FileProvider.getUriForFile(context,authority,source)
        val outputUri=FileProvider.getUriForFile(context,authority,destination)
        val pickerAction=ActivityResultContracts.PickVisualMedia().createIntent(context,PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)).action!!
        lateinit var model:CompressionViewModel
        compose.activityRule.scenario.onActivity { model=ViewModelProvider(it)[CompressionViewModel::class.java] }
        val original=source.readBytes()
        Intents.init()
        try {
            Intents.intending(hasAction(pickerAction)).respondWith(ActivityResult(Activity.RESULT_OK,Intent().setData(inputUri)))
            Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_OK,Intent().setData(outputUri)))
            Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_CANCELED,null))
            compose.onNodeWithText("Select image").performClick()
            compose.waitUntil(30000) { model.state.value.source != null && !model.state.value.busy }
            compose.onNodeWithText("Compress image").assertIsDisplayed().performClick()
            compose.waitUntil(60000) { model.state.value.result != null && !model.state.value.busy }
            compose.onNodeWithText("After · Compressed").performScrollTo().assertIsDisplayed()
            assertTrue(model.state.value.result!!.meetsTarget)
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) { com.komprexo.app.ui.KomprexoScreen() }
                }
            }
            compose.waitForIdle()
            compose.onNodeWithText("Before · Original").performScrollTo().performClick()
            compose.onNodeWithTag("comparisonPreview").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("After").performScrollTo().performClick()
            compose.onNodeWithTag("saveAction").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag("shareAction").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            captureEvidence("result-large-font")

            compose.onNodeWithText("Save a copy").assertIsDisplayed().performClick()
            compose.waitUntil(30000) { model.state.value.saved }
            assertArrayEquals(model.state.value.result!!.file.readBytes(),destination.readBytes())
            compose.onNodeWithText("Share").assertIsDisplayed().performClick()
            compose.waitUntil(30000) { !model.state.value.busy }
            Intents.intended(hasAction(Intent.ACTION_CHOOSER))
            assertArrayEquals(original,source.readBytes())
        } finally { Intents.release();source.delete();destination.delete() }
    }
}
