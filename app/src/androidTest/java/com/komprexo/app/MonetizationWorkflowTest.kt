package com.komprexo.app

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.komprexo.app.access.*
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import com.komprexo.app.ui.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Free production policy over real Gallery URIs and native engines; separate journal per test. */
class MonetizationWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var quota: DailyQuotaManager
    private lateinit var single: CompressionViewModel
    private lateinit var multi: Phase2ViewModel
    private lateinit var store: MemoryQuotaStore
    private fun main(block: ()->Unit) { compose.activityRule.scenario.onActivity { block() } }
    private fun gallery(name: String)=Uri.parse("content://com.komprexo.app.test.gallery/$name")
    @Before fun setup() {
        EntitlementProviderFactory.setTestingPremium(false)
        store=MemoryQuotaStore();quota=DailyQuotaManager(store,EntitlementProviderFactory.provider)
        val app=compose.activity.application as Application
        single=CompressionViewModel(app,SavedStateHandle(),quota);multi=Phase2ViewModel(app,SavedStateHandle(),quota)
        main { compose.activity.setContent { KomprexoApp(single,multi) } }
        compose.waitForIdle()
    }
    @After fun cleanup() {
        main { compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        single.viewModelScope.cancel();multi.viewModelScope.cancel()
        single.state.value.source?.file?.delete();single.state.value.result?.file?.parentFile?.deleteRecursively()
        multi.state.value.selection.forEach { it.source?.file?.delete() };multi.state.value.outputs.forEach { it.file.parentFile?.deleteRecursively() }
        EntitlementProviderFactory.setTestingPremium(false)
    }
    private fun selectedSingle() {
        compose.onNodeWithTag("homecompress").performScrollTo().performClick()
        main { single.select(gallery("stream")) }
        compose.waitUntil(30000) { single.state.value.source!=null && !single.state.value.busy }
    }
    private fun selectMulti(tool: String, count: Int=1, names: List<String> = List(count) { "stream" }) {
        compose.onNodeWithTag("home$tool").performScrollTo().performClick()
        main { if(tool!="BATCH") multi.setMultiple(count>1);multi.select(names.map(::gallery)) }
        compose.waitUntil(30000) { multi.state.value.selection.size==count && !multi.state.value.busy }
    }
    private fun processMulti() {
        compose.onNodeWithTag("phase2Start").performClick()
        compose.waitUntil(60000) { !multi.state.value.busy }
    }
    private fun largeFont() {
        main { compose.activity.setContent {
            val d=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density,2f)) { KomprexoApp(single,multi) }
        } }
    }
    @Test fun fiveSingleSuccessesBothModesThenUpgradeKeepsResult() {
        selectedSingle();val original=single.state.value.source!!.file.readBytes()
        repeat(5) { index ->
            val old=single.state.value.result
            main { single.compress(200*1024,OutputFormat.AUTO,if(index%2==0) CompressionMode.QUALITY_FIRST else CompressionMode.BALANCED) }
            compose.waitUntil(60000) { !single.state.value.busy && single.state.value.result!==old }
            assertTrue(single.state.value.result!!.meetsTarget)
        }
        assertEquals(5,store.ledger.used);assertArrayEquals(original,single.state.value.source!!.file.readBytes())
        main { single.compress(200*1024,OutputFormat.AUTO) }
        compose.waitUntil(30000) { !single.state.value.busy }
        compose.onNodeWithTag("purchasePremium").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("premiumNotice").performScrollTo().assertTextEquals("Premium purchases are not available in this test build.")
        compose.onNodeWithTag("dismissPremium").performClick()
        compose.onNodeWithTag("saveAction").assertIsDisplayed();compose.onNodeWithTag("shareAction").assertIsDisplayed()
        assertEquals(5,store.ledger.used)
    }
    @Test fun singleAndBatchShareCreditsAndPartialFailure() {
        selectedSingle()
        main { single.compress(200*1024,OutputFormat.AUTO) }
        compose.waitUntil(60000) { single.state.value.result!=null && !single.state.value.busy }
        compose.onNodeWithTag("homeNavigation").performClick()
        selectMulti("BATCH",2,listOf("stream","heif"));processMulti()
        assertEquals(1,multi.state.value.outputs.size);assertEquals(1,multi.state.value.progress!!.failed)
        assertEquals(2,store.ledger.used)
        compose.onNodeWithTag("phase2Save").assertIsDisplayed();compose.onNodeWithTag("phase2Share").assertIsDisplayed()
    }
    @Test fun failedCompressionDoesNotCharge() {
        selectedSingle();main { single.compress(0,OutputFormat.AUTO) }
        compose.waitUntil(30000) { !single.state.value.busy }
        assertNull(single.state.value.result);assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.reserved)
    }
    @Test fun cancelAtReservationBoundaryReleasesCredit() {
        selectedSingle()
        main { single.compress(1,OutputFormat.PNG);single.cancel() }
        compose.waitUntil(30000) { !single.state.value.busy }
        assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.reserved)
    }
    @Test fun doubleTapConsumesOneCredit() {
        selectedSingle();main { repeat(2) { single.compress(200*1024,OutputFormat.AUTO) } }
        compose.waitUntil(60000) { single.state.value.result!=null && !single.state.value.busy }
        assertEquals(1,store.ledger.used)
    }
    @Test fun simultaneousViewModelsCannotBypassQuota() {
        selectedSingle();compose.onNodeWithTag("homeNavigation").performClick();selectMulti("BATCH",2)
        main { single.compress(200*1024,OutputFormat.AUTO);multi.start() }
        compose.waitUntil(60000) { !single.state.value.busy && !multi.state.value.busy }
        assertNotNull(single.state.value.result);assertTrue(multi.state.value.outputs.isEmpty())
        assertEquals(Restriction.BUSY,multi.state.value.restriction);assertEquals(1,store.ledger.used)
    }
    @Test fun resizeHasSeparateQuotaAfterCompressionExhaustion() {
        runBlocking { repeat(5) { val r=quota.reserve(Operation.COMPRESS,1);quota.settle(r,0);quota.release(r) } }
        selectMulti("RESIZE");processMulti();assertEquals(1,multi.state.value.outputs.size);assertEquals(5,store.ledger.used);assertEquals(1,store.ledger.resizeUsed)
        assertNull(multi.state.value.outputs.single().targetBytes)
    }
    @Test fun convertHasSeparateQuotaAfterCompressionExhaustion() {
        runBlocking { repeat(5) { val r=quota.reserve(Operation.COMPRESS,1);quota.settle(r,0);quota.release(r) } }
        selectMulti("CONVERT");processMulti();assertEquals(1,multi.state.value.outputs.size);assertEquals(5,store.ledger.used);assertEquals(1,store.ledger.convertUsed)
        assertEquals(OutputFormat.PNG,multi.state.value.outputs.single().format)
    }
    @Test fun batchResizeChargesOnlyResize() {
        selectMulti("RESIZE",2);processMulti();assertEquals(2,multi.state.value.outputs.size);assertEquals(0,store.ledger.used);assertEquals(2,store.ledger.resizeUsed);assertEquals(0,store.ledger.convertUsed)
        compose.onNodeWithTag("phase2Save").assertIsDisplayed();compose.onNodeWithTag("phase2Share").assertIsDisplayed()
    }
    @Test fun batchConvertChargesOnlyConvert() {
        selectMulti("CONVERT",2);processMulti();assertEquals(2,multi.state.value.outputs.size);assertEquals(0,store.ledger.used);assertEquals(2,store.ledger.convertUsed);assertEquals(0,store.ledger.resizeUsed)
        assertTrue(multi.state.value.outputs.all { it.format==OutputFormat.PNG })
    }
    private fun exhaustFeature(workflow: String, operation: Operation) {
        selectMulti(workflow)
        repeat(5) { index ->
            main { multi.edit(multi.state.value.settings.copy(format=if(index%2==0) OutputFormat.PNG else OutputFormat.WEBP)) }
            // Settings and action may be below the newly added quota indicator in the compact viewport.
            if(multi.state.value.outputs.isNotEmpty()) compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("phase2Start"))
            processMulti()
            assertEquals(index+1,store.ledger.used(operation));assertEquals(1,multi.state.value.outputs.size)
        }
        val source=multi.state.value.selection.single().source!!.file
        val result=multi.state.value.outputs.single().file
        if(multi.state.value.outputs.isNotEmpty()) compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("phase2Start"));processMulti()
        compose.onNodeWithTag("purchasePremium").assertIsNotEnabled()
        compose.onNodeWithTag("dismissPremium").performClick()
        assertTrue(source.exists());assertTrue(result.exists());assertEquals(5,store.ledger.used(operation))
        compose.onNodeWithTag("phase2Save").assertIsDisplayed();compose.onNodeWithTag("phase2Share").assertIsDisplayed()
        assertEquals(0,store.ledger.used)
    }
    @Test fun resizeFiveSuccessfulImagesThenUpgradePreservesOutput() { exhaustFeature("RESIZE",Operation.RESIZE) }
    @Test fun convertFiveSuccessfulImagesThenUpgradePreservesOutput() { exhaustFeature("CONVERT",Operation.CONVERT) }
    @Test fun resizeAndConvertShareSingleBatchCounterPerFeature() {
        selectMulti("RESIZE",2);processMulti();assertEquals(2,store.ledger.resizeUsed)
        main { multi.remove(multi.state.value.selection.last().id);multi.setMultiple(false) }
        if(multi.state.value.outputs.isNotEmpty()) compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("phase2Start"));processMulti();assertEquals(3,store.ledger.resizeUsed)
        assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.convertUsed)
        compose.onNodeWithTag("homeNavigation").performClick();compose.onNodeWithTag("homeCONVERT").performScrollTo().performClick()
        compose.onNodeWithTag("confirmSwitch").performClick()
        main { multi.select(listOf(gallery("stream"))); }
        compose.waitUntil(30000) { multi.state.value.selection.size==1 && !multi.state.value.busy }
        processMulti();assertEquals(1,store.ledger.convertUsed);assertEquals(3,store.ledger.resizeUsed)
    }
    @Test fun resizeEncodingFormatChargesOnceAndCompressionResizeChargesCompressionOnly() {
        selectedSingle();main { single.compress(200*1024,OutputFormat.WEBP,resize=ResizeSpec.Percent(50)) }
        compose.waitUntil(60000) { single.state.value.result!=null && !single.state.value.busy }
        assertEquals(1,store.ledger.used);assertEquals(0,store.ledger.resizeUsed);assertEquals(0,store.ledger.convertUsed)
        compose.onNodeWithTag("homeNavigation").performClick();selectMulti("RESIZE")
        main { multi.edit(multi.state.value.settings.copy(format=OutputFormat.WEBP,resizeChoice=ResizeChoice.PERCENT,percent="50")) }
        processMulti();assertEquals(1,store.ledger.resizeUsed);assertEquals(1,store.ledger.used);assertEquals(0,store.ledger.convertUsed)
    }
    @Test fun partialBatchConvertChargesOnlyAcceptedSuccess() {
        selectMulti("CONVERT",2,listOf("stream","heif"));processMulti()
        assertEquals(1,multi.state.value.outputs.size);assertEquals(1,multi.state.value.progress!!.failed)
        assertEquals(1,store.ledger.convertUsed);assertEquals(0,store.ledger.used);assertEquals(0,store.ledger.resizeUsed)
    }
    @Test fun failedResizeAndImmediateCancellationDoNotCharge() {
        selectMulti("RESIZE")
        main { multi.edit(multi.state.value.settings.copy(resizeChoice=ResizeChoice.CUSTOM,width="9999",height="9999"));multi.start() }
        compose.waitUntil(30000) { !multi.state.value.busy }
        assertTrue(multi.state.value.outputs.isEmpty());assertEquals(0,store.ledger.resizeUsed)
        main { multi.edit(multi.state.value.settings.copy(resizeChoice=ResizeChoice.ORIGINAL));multi.start();multi.cancel() }
        compose.waitUntil(30000) { !multi.state.value.busy }
        assertEquals(0,store.ledger.resizeUsed);assertEquals(0,store.ledger.reserved)
    }
    @Test fun homeQuotaIndicatorShowsThreeIndependentAllowances() {
        runBlocking {
            val resize=quota.reserve(Operation.RESIZE,1);quota.settle(resize,0);quota.release(resize)
            val convert=quota.reserve(Operation.CONVERT,2);quota.settle(convert,0);quota.settle(convert,1);quota.release(convert)
        }
        compose.onNodeWithText("Free today — Compress 5/5 · Resize 4/5 · Convert 3/5.").assertIsDisplayed()
    }
    @Test fun oversizedBatchInvitationPreservesSelectionAndSettings() {
        selectMulti("BATCH",1)
        val source=multi.state.value.selection.single().source!!.file
        main { multi.edit(multi.state.value.settings.copy(targetKiB="750"));multi.select(List(2) { gallery("stream") }) }
        compose.waitForIdle();compose.onNodeWithTag("purchasePremium").assertIsNotEnabled()
        compose.onNodeWithTag("dismissPremium").performClick();assertEquals(1,multi.state.value.selection.size);assertTrue(source.exists());assertEquals("750",multi.state.value.settings.targetKiB)
        compose.onNodeWithTag("phase2Select").assertIsDisplayed()
    }
    @Test fun premiumPresetIsMarkedAndDoesNotApplyOnFree() {
        selectMulti("BATCH");compose.onNodeWithTag("phase2List").performScrollToNode(hasTestTag("toolSettings"));compose.onNodeWithTag("toolSettings").performClick()
        compose.onNodeWithText("Website · Premium").performScrollTo().performClick()
        compose.onNodeWithTag("purchasePremium").assertIsNotEnabled();assertEquals(Preset.CUSTOM,multi.state.value.settings.preset)
        compose.onNodeWithTag("dismissPremium").performClick();compose.onNodeWithTag("settingsDone").performClick()
        assertEquals(1,multi.state.value.selection.size)
    }
    @Test fun premiumScreenCompactLargeFontLandscape() {
        largeFont();compose.onNodeWithTag("upgrade").performScrollTo().performClick()
        main { compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE }
        // Orientation recreates Activity: restore explicit test root with actual 200% font scale.
        main { compose.activity.setContent {
            val d=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density,2f)) { PremiumScreen(onBack={}) }
        } }
        compose.onNodeWithTag("purchasePremium").assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertIsNotEnabled()
        compose.onNodeWithTag("dismissPremium").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("premiumNotice").performScrollTo().assertIsDisplayed()
        captureEvidence("phase3-premium-landscape-font200")
    }
    @Test fun premiumSystemBarsContrastInBothThemes() {
        for(dark in listOf(false,true)) {
            main { compose.activity.setContent {
                val configuration=Configuration(LocalConfiguration.current).apply {
                    uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) { KomprexoApp(single,multi) }
            } }
            compose.onNodeWithTag("upgrade").performScrollTo().performClick()
            compose.onNodeWithTag("purchasePremium").assertIsDisplayed().assertIsNotEnabled()
            captureEvidence(if(dark) "phase3-premium-dark" else "phase3-premium-light",darkTheme=dark)
            compose.onNodeWithTag("dismissPremium").performClick()
        }
    }
    @Test fun premiumNoticeIndonesianAndDebugTestingExplicit() {
        main { compose.activity.setContent {
            val config=Configuration(LocalConfiguration.current).apply { setLocale(java.util.Locale("in")) }
            CompositionLocalProvider(LocalConfiguration provides config, androidx.compose.ui.platform.LocalContext provides compose.activity.createConfigurationContext(config)) { PremiumScreen(onBack={}) }
        } }
        compose.onNodeWithTag("premiumNotice").performScrollTo().assertTextEquals("Pembelian Premium belum tersedia pada versi pengujian ini.")
        compose.onNodeWithTag("debugEntitlement").performScrollTo().performClick()
        assertEquals(EntitlementState.Premium,EntitlementProviderFactory.provider.state.value)
        compose.onNodeWithTag("purchasePremium").assertIsNotEnabled()
    }
    @Test fun downgradeChecksWorkflowNotOnlyButtons() {
        EntitlementProviderFactory.setTestingPremium(true);selectMulti("BATCH",3)
        EntitlementProviderFactory.setTestingPremium(false)
        main { multi.start() }
        compose.waitUntil(30000) { !multi.state.value.busy }
        compose.onNodeWithTag("purchasePremium").assertIsNotEnabled()
        assertEquals(3,multi.state.value.selection.size);assertTrue(multi.state.value.outputs.isEmpty());assertEquals(0,store.ledger.used)
    }
}
