package com.komprexo.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.komprexo.app.compression.OutputFormat
import com.komprexo.app.ui.*
import org.junit.*
import org.junit.Assert.*

/** Real responsive dialog and navigation UI; no image-processing behavior is replaced. */
class UiStabilizationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun model(): Phase2ViewModel {
        lateinit var value: Phase2ViewModel
        compose.activityRule.scenario.onActivity { value = ViewModelProvider(it)[Phase2ViewModel::class.java] }
        return value
    }
    private fun largeFont() {
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) { KomprexoApp() }
        } }
    }
    private fun landscape() {
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
    }
    @After fun restoreDisplay() {
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    private fun openSettings(tool: String) {
        compose.onNodeWithTag("home$tool").performScrollTo().performClick()
        compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("toolSettings"))
        compose.onNodeWithTag("toolSettings").performClick()
        compose.onNodeWithTag("settingsDone").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }
    private fun visibleChoicesDoNotOverlap() {
        val choices = compose.onAllNodes(isSelectable()).fetchSemanticsNodes().map { it.boundsInRoot }.filter { it.width > 0 && it.height > 0 }
        assertTrue("At least one choice must be visible", choices.isNotEmpty())
        for (i in choices.indices) for (j in i+1 until choices.size) {
            val a=choices[i];val b=choices[j]
            assertFalse("Choices overlap: $a / $b", a.overlaps(b))
        }
        val dialogOpen=compose.onAllNodesWithTag("settingsScroll").fetchSemanticsNodes().isNotEmpty()
        val activeText=SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and
            if(dialogOpen) hasAnyAncestor(hasTestTag("settingsScroll")) else SemanticsMatcher("Active page") { true }
        val textNodes=compose.onAllNodes(activeText,useUnmergedTree=true).fetchSemanticsNodes()
        compose.runOnIdle {
            for(node in textNodes.filter { it.boundsInRoot.width>=it.size.width-1 && it.boundsInRoot.height>=it.size.height-1 && it.size.height>0 }) {
                val layouts=mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { assertFalse("Visible label is clipped: ${it.layoutInput.text}; size=${it.size}, widthOverflow=${it.didOverflowWidth}, heightOverflow=${it.didOverflowHeight}",it.hasVisualOverflow) }
            }
        }
        for(node in compose.onAllNodes(isSelectable()).fetchSemanticsNodes()) {
            if(node.boundsInRoot.width>0 && node.boundsInRoot.height>0) {
                // Touch target geometry, not only semantics labels.
                val min=48 * compose.activity.resources.displayMetrics.density
                assertTrue(node.size.height >= min-1)
            }
        }
    }
    @Test fun resizeDialogScrollsAndKeepsEditsOnDismissal() {
        openSettings("RESIZE")
        compose.onNodeWithTag("customDimensions").performScrollTo().performClick()
        compose.onNodeWithTag("resizeWidth").performScrollTo().performTextInput("120")
        compose.onNodeWithTag("resizeHeight").performScrollTo().performTextInput("80")
        compose.onNodeWithTag("aspectLock").performScrollTo().performClick()
        compose.onNodeWithTag("settingsDone").assertIsDisplayed().performClick()
        val model=model();assertEquals("120",model.state.value.settings.width);assertEquals("80",model.state.value.settings.height)
        assertFalse(model.state.value.settings.lockAspect)
        compose.onNodeWithTag("toolSettings").performClick()
        compose.onNodeWithTag("resizeHeight").performScrollTo().assertTextContains("80")
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("settingsDialog").assertDoesNotExist()
        compose.onNodeWithTag("toolSettings").performClick()
        compose.onNodeWithTag("resizeWidth").performScrollTo().assertTextContains("120")
        compose.onNodeWithTag("settingsDone").assertIsDisplayed()
        compose.onNodeWithTag("settingsBackdrop").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f,2f)) }
        compose.onNodeWithTag("settingsDialog").assertDoesNotExist()
        assertEquals("120",model.state.value.settings.width)
    }
    @Test fun batchDialogAt200PercentWrapsChoicesAndKeepsDoneVisible() {
        largeFont();openSettings("BATCH")
        compose.onNodeWithTag("phase2CustomTarget").performScrollTo().assertHeightIsAtLeast(48.dp)
        visibleChoicesDoNotOverlap()
        compose.onNodeWithTag("phase2CustomTarget").performScrollTo().performClick()
        compose.onNodeWithTag("phase2Target").performScrollTo().performTextReplacement("750")
        compose.onNodeWithText("Balanced").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("WebP",ignoreCase=true).performScrollTo().performClick().assertIsSelected()
        visibleChoicesDoNotOverlap()
        compose.onNodeWithTag("settingsDone").assertIsDisplayed()
        captureEvidence("phase25-batch-dialog-font200")
        compose.onNodeWithTag("settingsDone").performClick()
        assertEquals("750",model().state.value.settings.targetKiB);assertEquals(OutputFormat.WEBP,model().state.value.settings.format)
    }
    @Test fun converterLandscapeAt200PercentHasSeparateNavigationAndResponsiveDialog() {
        landscape();largeFont();openSettings("CONVERT")
        compose.onNodeWithText("JPEG").performScrollTo().performClick().assertIsSelected()
        visibleChoicesDoNotOverlap()
        captureEvidence("phase25-converter-landscape-font200")
        compose.onNodeWithTag("settingsDone").assertIsDisplayed().performClick()
        compose.onNodeWithTag("pageTitle").assertTextEquals("Convert")
        compose.onNodeWithTag("homeNavigation").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        assertFalse(compose.onNodeWithTag("pageTitle").fetchSemanticsNode().boundsInRoot.overlaps(
            compose.onNodeWithTag("homeNavigation").fetchSemanticsNode().boundsInRoot))
        assertEquals(OutputFormat.JPEG,model().state.value.settings.format)
    }
    @Test fun resizeLandscapeAt200PercentKeepsDimensionControlsReachable() {
        landscape();largeFont();openSettings("RESIZE")
        compose.onNodeWithTag("customDimensions").performScrollTo().performClick()
        compose.onNodeWithTag("resizeWidth").performScrollTo().performTextInput("123")
        compose.onNodeWithTag("resizeHeight").performScrollTo().performTextInput("45")
        compose.onNodeWithTag("aspectLock").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settingsDone").assertIsDisplayed()
        captureEvidence("phase25-resize-landscape-font200")
        compose.onNodeWithTag("settingsDone").performClick()
        val model=model()
        assertEquals("123",model.state.value.settings.width);assertEquals("45",model.state.value.settings.height)
        compose.activityRule.scenario.onActivity { model.select(listOf(Uri.parse("content://com.komprexo.app.test.gallery/stream"))) }
        compose.waitUntil(30000) { model.state.value.selection.size==1 && !model.state.value.busy }
        compose.onNodeWithTag("phase2Start").performClick()
        compose.waitUntil(30000) { model.state.value.outputs.size==1 && !model.state.value.busy }
        for(tag in listOf("phase2Save","phase2Share")) {
            val action=compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp).fetchSemanticsNode()
            assertTrue("Export action must be fully visible",action.boundsInRoot.height>=action.size.height-1)
            assertTrue("Export action must not overlap navigation",compose.onNodeWithTag("homeNavigation").fetchSemanticsNode().boundsInRoot.bottom<=action.boundsInRoot.top)
        }
        captureEvidence("phase25-resize-landscape-result-font200")
    }
    @Test fun compressionChoicesAndNavigationRetainSettingsAt200Percent() {
        largeFont();compose.onNodeWithTag("homecompress").performScrollTo().performClick()
        compose.onNodeWithText("Custom").performScrollTo().performClick().assertIsSelected()
        visibleChoicesDoNotOverlap()
        compose.onNodeWithTag("customSize").performScrollTo().performTextInput("750")
        compose.onNodeWithText("Balanced").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Auto · recommended").performScrollTo().assertIsDisplayed()
        visibleChoicesDoNotOverlap()
        captureEvidence("phase25-compress-font200")
        compose.onNodeWithTag("homeNavigation").performClick()
        compose.onNodeWithTag("homecompress").performScrollTo().performClick()
        compose.onNodeWithTag("customSize").performScrollTo().assertTextContains("750")
        compose.onNodeWithText("Balanced").performScrollTo().assertIsSelected()
    }
    @Test fun normalRootRecreationKeepsOpenDialogAndSettings() {
        openSettings("RESIZE")
        compose.onNodeWithTag("customDimensions").performScrollTo().performClick()
        compose.onNodeWithTag("resizeWidth").performScrollTo().performTextInput("120")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("settingsDone").assertIsDisplayed()
        compose.onNodeWithTag("resizeWidth").performScrollTo().assertTextContains("120")
        compose.onNodeWithTag("settingsDone").performClick()
    }
    @Test fun changingToolsRequiresExplicitDiscardAndCancelPreservesSelection() {
        compose.onNodeWithTag("homeBATCH").performClick();val model=model()
        compose.activityRule.scenario.onActivity { model.select(listOf(Uri.parse("content://com.komprexo.app.test.gallery/stream"))) }
        compose.waitUntil(30000) { model.state.value.selection.size==1 && !model.state.value.busy }
        val original=model.state.value.selection.single().source!!.file
        compose.activityRule.scenario.onActivity { model.edit(model.state.value.settings.copy(targetKiB="750")) }
        compose.onNodeWithTag("homeNavigation").performClick()
        compose.onNodeWithTag("homeRESIZE").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(original.exists());assertEquals("750",model.state.value.settings.targetKiB)
        compose.onNodeWithTag("homeBATCH").performClick()
        compose.onNodeWithTag("selectionCount").assertTextEquals("Selected 1 / 20")
        compose.onNodeWithTag("homeNavigation").performClick()
        compose.onNodeWithTag("homeRESIZE").performClick()
        compose.onNodeWithTag("confirmSwitch").performClick()
        compose.onNodeWithTag("pageTitle").assertTextEquals("Resize")
        assertTrue(model.state.value.selection.isEmpty());assertFalse(original.exists())
    }
    @Test fun dialogSystemBarsRemainVisibleInLightAndDarkThemes() {
        for(dark in listOf(false,true)) {
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                val configuration=Configuration(LocalConfiguration.current).apply {
                    uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) { KomprexoApp() }
            } }
            openSettings("BATCH")
            captureEvidence(if(dark) "phase25-dialog-dark" else "phase25-dialog-light",darkTheme=dark)
            compose.onNodeWithTag("settingsDone").performClick()
            compose.onNodeWithTag("homeNavigation").performClick()
        }
    }
    @Test fun actualKeyboardShrinksDialogWithoutHidingConfirmation() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        fun command(text: String) = instrumentation.uiAutomation.executeShellCommand(text).use { fd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes().toString(Charsets.UTF_8).trim() }
        }
        val previous=command("settings get secure show_ime_with_hard_keyboard")
        command("settings put secure show_ime_with_hard_keyboard 1")
        try {
            compose.activityRule.scenario.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    var settings by rememberSaveable(stateSaver=EditableSettingsSaver) { mutableStateOf(EditableSettings(resizeChoice=ResizeChoice.CUSTOM)) }
                    ResponsiveSettingsDialog("Resize",onDismiss={},onConfirm={}) {
                        val density=LocalDensity.current
                        Text("${WindowInsets.ime.getBottom(density)}",modifier=Modifier.testTag("imeHeight"))
                        ResizeControls(settings,true) { settings=it }
                    }
                }
            } }
            compose.onNodeWithTag("resizeWidth").performScrollTo().performClick()
            compose.waitUntil(15000) {
                val text=compose.onNodeWithTag("imeHeight").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
                (text.toIntOrNull() ?: 0)>0
            }
            compose.onNodeWithTag("resizeWidth").performTextInput("120")
            compose.onNodeWithTag("resizeHeight").performScrollTo().performTextInput("80")
            compose.onNodeWithTag("settingsDone").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            captureEvidence("phase25-dialog-real-ime")
            compose.onNodeWithTag("settingsDone").performClick()
            compose.onNodeWithTag("resizeWidth").performScrollTo().assertTextContains("120")
            compose.onNodeWithTag("resizeHeight").performScrollTo().assertTextContains("80")
        } finally {
            if(previous=="null") command("settings delete secure show_ime_with_hard_keyboard")
            else command("settings put secure show_ime_with_hard_keyboard $previous")
        }
    }
}
