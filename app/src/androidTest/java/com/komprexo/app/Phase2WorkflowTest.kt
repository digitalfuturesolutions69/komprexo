package com.komprexo.app

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import com.komprexo.app.ui.*
import org.junit.*
import org.junit.Assert.*

class Phase2WorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun model(): Phase2ViewModel {
        lateinit var model: Phase2ViewModel
        compose.activityRule.scenario.onActivity { model=ViewModelProvider(it)[Phase2ViewModel::class.java] }
        return model
    }
    private fun gallery(name: String)=Uri.parse("content://com.komprexo.app.test.gallery/$name")
    @Test fun compactHomeShowsAllTools() {
        for(tag in listOf("homecompress","homeBATCH","homeCONVERT","homeRESIZE")) compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("homeCONVERT").performClick()
        compose.onNodeWithTag("phase2Select").assertIsDisplayed()
        compose.onNodeWithTag("phase2Start").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithTag("homeRESIZE").assertIsDisplayed()
    }
    @Test fun batchPickerPartialResultsFolderSaveShareAndClear() {
        compose.onNodeWithTag("homeBATCH").performClick();val model=model()
        val uris=listOf(gallery("stream"),gallery("png"),gallery("heif"))
        val clip=ClipData.newRawUri("Fixtures",uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        val pickerAction=ActivityResultContracts.PickMultipleVisualMedia(20).createIntent(compose.activity,PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)).action!!
        val tree=DocumentsContract.buildTreeDocumentUri(DocumentsFixtureProvider.AUTHORITY,"ui-${java.util.UUID.randomUUID()}")
        Intents.init()
        try {
            Intents.intending(hasAction(pickerAction)).respondWith(ActivityResult(Activity.RESULT_OK,Intent().apply { data=uris[0];clipData=clip }))
            Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE)).respondWith(ActivityResult(Activity.RESULT_OK,Intent().setData(tree)))
            Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_CANCELED,null))
            compose.onNodeWithTag("phase2Select").performClick()
            compose.waitUntil(30000) { model.state.value.selection.size==3 && !model.state.value.busy }
            compose.onNodeWithTag("selectionCount").assertTextEquals("Selected 3 / 20")
            assertEquals(FailureCode.UNSUPPORTED_HEIF,model.state.value.selection[2].error)
            assertTrue(model.state.value.selection.take(2).all { it.thumbnail!!.width<=160 && it.thumbnail!!.height<=160 })
            compose.activityRule.scenario.onActivity { model.select(List(21) { gallery("stream") }) }
            assertEquals(3,model.state.value.selection.size);assertEquals(FailureCode.BATCH_LIMIT,model.state.value.error)
            compose.onNodeWithTag("phase2Start").performClick()
            compose.waitUntil(60000) { model.state.value.progress?.percent==100 && !model.state.value.busy }
            assertEquals(2,model.state.value.progress!!.completed);assertEquals(1,model.state.value.progress!!.failed)
            assertTrue(model.state.value.outputs.all { it.meetsTarget==true })
            compose.onNodeWithTag("phase2Save").assertIsDisplayed().performClick()
            compose.waitUntil(30000) { model.state.value.exports.size==2 && !model.state.value.busy }
            assertTrue(model.state.value.exports.all { it.destination!=null })
            compose.onNodeWithTag("phase2Share").assertIsDisplayed().performClick()
            compose.waitUntil(30000) { !model.state.value.busy }
            Intents.intended(hasAction(Intent.ACTION_CHOOSER))
            val chooser=Intents.getIntents().last { it.action==Intent.ACTION_CHOOSER }
            val shared=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND_MULTIPLE,shared.action);assertEquals(2,shared.clipData!!.itemCount)
            val files=model.state.value.selection.mapNotNull { it.source?.file } + model.state.value.outputs.map { it.file }
            val exported=model.state.value.exports.map { it.destination!! }
            compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("remove2"))
            compose.onNodeWithTag("remove2").performClick()
            assertEquals(2,model.state.value.selection.size)
            compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("clearSelection"))
            compose.onNodeWithTag("clearSelection").performClick()
            assertTrue(model.state.value.selection.isEmpty());assertTrue(files.none { it.exists() })
            exported.forEach { uri->compose.activity.contentResolver.openInputStream(uri)!!.use { assertTrue(it.read()!=-1) } }
        } finally { Intents.release() }
    }
    @Test fun converterRequiresAlphaConfirmation() {
        compose.onNodeWithTag("homeCONVERT").performClick();val model=model()
        compose.activityRule.scenario.onActivity { model.select(listOf(gallery("png"))) }
        compose.waitUntil(30000) { model.state.value.selection.size==1 && !model.state.value.busy }
        compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("toolSettings"))
        compose.onNodeWithTag("toolSettings").performClick()
        compose.onNodeWithText("JPEG").performScrollTo().performClick()
        compose.onNodeWithTag("settingsDone").performClick()
        compose.onNodeWithTag("phase2Start").performClick()
        assertEquals(FailureCode.ALPHA_CONFIRMATION,model.state.value.error);assertTrue(model.state.value.outputs.isEmpty())
        compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("alphaConfirmation"))
        compose.onNodeWithTag("alphaConfirmation").performClick()
        compose.onNodeWithTag("phase2Start").performClick()
        compose.waitUntil(30000) { model.state.value.outputs.size==1 && !model.state.value.busy }
        val output=model.state.value.outputs.single()
        assertEquals(OutputFormat.JPEG,output.format);assertTrue(output.transparencyRemoved);assertNull(output.targetBytes)
        val destination=DocumentsContract.buildDocumentUri(DocumentsFixtureProvider.AUTHORITY,"single-${java.util.UUID.randomUUID()}.jpg")
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_OK,Intent().setData(destination)))
            Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_CANCELED,null))
            compose.onNodeWithTag("phase2Save").assertIsDisplayed().performClick()
            compose.waitUntil(30000) { model.state.value.exports.isNotEmpty() && !model.state.value.busy }
            compose.activity.contentResolver.openInputStream(destination)!!.use { assertArrayEquals(output.file.readBytes(),it.readBytes()) }
            compose.onNodeWithTag("phase2Share").assertIsDisplayed().performClick()
            compose.waitUntil(30000) { !model.state.value.busy }
            val chooser=Intents.getIntents().last { it.action==Intent.ACTION_CHOOSER }
            val shared=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND,shared.action);assertEquals("image/jpeg",shared.type);assertEquals(1,shared.clipData!!.itemCount)
        } finally { Intents.release() }
    }
    @Test fun resizeLargeFontKeepsExportAccessible() {
        compose.activityRule.scenario.onActivity { activity->activity.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)) { KomprexoApp() }
        } }
        compose.onNodeWithTag("homeRESIZE").performScrollTo().performClick();val model=model()
        compose.activityRule.scenario.onActivity { model.select(listOf(gallery("stream"))) }
        compose.waitUntil(30000) { model.state.value.selection.size==1 && !model.state.value.busy }
        compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("toolSettings"))
        compose.onNodeWithTag("toolSettings").performClick()
        compose.onNodeWithTag("customDimensions").performScrollTo().performClick()
        compose.onNodeWithTag("resizeWidth").performScrollTo().performTextInput("120")
        compose.onNodeWithTag("settingsDone").performClick()
        assertTrue(model.state.value.settings.lockAspect)
        compose.onNodeWithTag("phase2Start").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.waitUntil(30000) { model.state.value.outputs.size==1 && !model.state.value.busy }
        assertEquals(120,model.state.value.outputs.single().width);assertNull(model.state.value.outputs.single().targetBytes)
        compose.onNodeWithTag("phase2Save").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("phase2Share").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        captureEvidence("phase2-resize-large-font")
    }
    @Test fun resizeRotationKeepsOutputAndRoute() {
        // Normal production root: the synthetic density wrapper above has a
        // different rememberSaveable position from MainActivity.onCreate.
        compose.onNodeWithTag("homeRESIZE").performClick();val model=model()
        compose.activityRule.scenario.onActivity { model.select(listOf(gallery("stream"))) }
        compose.waitUntil(30000) { model.state.value.selection.size==1 && !model.state.value.busy }
        compose.onNodeWithTag("phase2Start").performClick()
        compose.waitUntil(30000) { model.state.value.outputs.size==1 && !model.state.value.busy }
        val file=model.state.value.outputs.single().file
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("phase2Save").assertIsDisplayed()
        compose.onNodeWithTag("phase2Share").assertIsDisplayed()
        assertTrue(file.exists())
    }
    @Test fun batchCancelKeepsControlsAvailable() {
        compose.onNodeWithTag("homeBATCH").performClick();val model=model()
        compose.activityRule.scenario.onActivity { model.select(List(10) { gallery("stream") }) }
        compose.waitUntil(30000) { model.state.value.selection.size==10 && !model.state.value.busy }
        compose.activityRule.scenario.onActivity { model.edit(model.state.value.settings.copy(targetKiB="1",format=OutputFormat.PNG)) }
        compose.onNodeWithTag("phase2Start").performClick()
        compose.waitUntil(10000) { model.state.value.busy }
        compose.onNodeWithTag("phase2Cancel").assertIsDisplayed().performClick()
        compose.waitUntil(30000) { !model.state.value.busy }
        assertEquals(FailureCode.CANCELLED,model.state.value.error)
        assertTrue(model.state.value.progress!!.items.any { it.result==ItemResult.Cancelled })
        compose.onNodeWithTag("phase2Start").assertIsDisplayed()
    }
}
