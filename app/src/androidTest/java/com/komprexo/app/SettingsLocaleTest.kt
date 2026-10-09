package com.komprexo.app

import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.net.Uri
import android.app.Activity
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.Intents.intended
import org.hamcrest.Matchers.allOf
import androidx.test.espresso.intent.Intents.intending
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.komprexo.app.access.*
import com.komprexo.app.billing.*
import com.komprexo.app.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Android locale/storage/UI tests. Fake Billing responses never create payments. */
class SettingsLocaleTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule(order=1) val failureEvidence=object: org.junit.rules.TestWatcher() {
        override fun failed(error:Throwable,description:org.junit.runner.Description) {
            runCatching { captureEvidence("phase4-failure-"+description.methodName) }
        }
    }
    private fun main(block:()->Unit)=InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    @Before fun english() { orientation(false);locale("en") }
    @After fun restore() {
        orientation(false)
        EntitlementProviderFactory.setTestingPremium(false)
        locale("")
    }
    @Suppress("DEPRECATION") private fun currentLanguage() = compose.activity.resources.configuration.let { if(android.os.Build.VERSION.SDK_INT>=24) it.locales[0].language else it.locale.language }
    private fun locale(tag:String) {
        val previous=compose.activity
        val effective=ConfigurationCompat.getLocales(previous.resources.configuration)[0]?.toLanguageTag()
        val expected=if(tag.isEmpty()) ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration)[0]!!.toLanguageTag()
            else java.util.Locale.forLanguageTag(tag).toLanguageTag()
        main { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) }
        // A real language change must finish recreation. A native same-language
        // regional/default change need not replace the Activity. Assert the stored
        // override separately; effective system locales can include regional fallback.
        try {
            compose.waitUntil(20000) {
                ConfigurationCompat.getLocales(compose.activity.resources.configuration)[0]?.let { actual ->
                    val wanted=java.util.Locale.forLanguageTag(expected)
                    actual.language==wanted.language && (wanted.country.isEmpty() || actual.country==wanted.country)
                }==true && AppCompatDelegate.getApplicationLocales().toLanguageTags()==tag &&
                    (java.util.Locale.forLanguageTag(effective ?: expected).language==java.util.Locale.forLanguageTag(expected).language || compose.activity!==previous) &&
                    compose.activity.lifecycle.currentState==Lifecycle.State.RESUMED
            }
        } catch(error: androidx.compose.ui.test.ComposeTimeoutException) {
            android.util.Log.i("KomprexoTest","event=LOCALE_TIMEOUT api="+android.os.Build.VERSION.SDK_INT+
                " requested="+tag+" expected="+expected+" before="+effective+
                " actual="+ConfigurationCompat.getLocales(compose.activity.resources.configuration).toLanguageTags()+
                " stored="+AppCompatDelegate.getApplicationLocales().toLanguageTags()+
                " replaced="+(compose.activity!==previous)+" lifecycle="+compose.activity.lifecycle.currentState)
            throw error
        }
        compose.waitForIdle()
    }
    private fun orientation(landscape:Boolean) {
        val previous=compose.activity
        val expected=if(landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        val changed=previous.resources.configuration.orientation!=expected
        main { compose.activity.requestedOrientation=if(landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(20000) {
            compose.activity.resources.configuration.orientation==expected &&
                (!changed || compose.activity!==previous) &&
                compose.activity.lifecycle.currentState==Lifecycle.State.RESUMED
        }
        compose.waitForIdle()
    }
    private fun settings() { compose.onNodeWithTag("homeSettings").performScrollTo().performClick() }
    private fun render(tag:String) {
        locale(tag);settings();compose.onNodeWithTag("languageSettings").performScrollTo().performClick()
        compose.onNodeWithTag("locale_hi").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("settingsDone").assertIsDisplayed().performClick()
        compose.onNodeWithTag("legal_privacy").performScrollTo().performClick()
        compose.onAllNodesWithText("komprexo.support@gmail.com",substring=true).onFirst().assertExists()
    }
    @Test fun englishSettingsAndLegal() { render("en") }
    @Test fun indonesianSettingsAndLegal() { render("id") }
    @Test fun spanishSettingsAndLegal() { render("es") }
    @Test fun portugueseSettingsAndLegal() { render("pt-BR") }
    @Test fun hindiSettingsAndLegal() { render("hi");captureEvidence("phase4-hindi-legal") }
    @Test fun systemDefaultChoiceClearsOverride() {
        locale("es");settings();compose.onNodeWithTag("languageSettings").performScrollTo().performClick()
        compose.onNodeWithTag("locale_").performScrollTo().performClick();compose.waitForIdle()
        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
    }
    @Test fun languagePersistsAfterActivityRecreation() {
        locale("pt-BR");compose.activityRule.scenario.recreate();compose.waitForIdle()
        assertEquals("pt",currentLanguage());assertEquals("pt-BR",AppCompatDelegate.getApplicationLocales().toLanguageTags())
    }
    @Test fun localeChangesPreserveWorkPremiumAndAllQuotaCounters() {
        EntitlementProviderFactory.setTestingPremium(true)
        lateinit var before: CompressionViewModel
        main { before=ViewModelProvider(compose.activity)[CompressionViewModel::class.java] }
        main { before.select(Uri.parse("content://com.komprexo.app.test.gallery/stream")) }
        compose.waitUntil(30000) { before.state.value.source!=null && !before.state.value.busy }
        main { before.compress(200*1024,com.komprexo.app.compression.OutputFormat.AUTO) }
        compose.waitUntil(60000) { before.state.value.result!=null && !before.state.value.busy }
        val result=before.state.value.result!!.file
        val original=before.state.value.source!!.file.readBytes()
        runBlocking { before.quota.refresh() };val counters=before.quota.state.value
        for(tag in listOf("id","es","pt-BR","hi","en")) {
            locale(tag)
            lateinit var after: CompressionViewModel
            main { after=ViewModelProvider(compose.activity)[CompressionViewModel::class.java] }
            assertSame(before,after);assertTrue(result.exists());assertArrayEquals(original,after.state.value.source!!.file.readBytes())
            assertEquals(counters,after.quota.state.value);assertEquals(EntitlementState.Premium,EntitlementProviderFactory.provider.state.value)
        }
    }
    @Test fun allOfflineLegalDocumentsExistInAllLanguages() {
        val context=compose.activity
        for(tag in listOf("en","id","es","pt-BR","hi")) for(page in listOf("privacy","terms","premium","licenses","about")) {
            val text=context.assets.open("legal/$tag/$page.txt").bufferedReader().use { it.readText() }
            assertTrue(text.isNotBlank())
            if(page!="licenses") assertTrue(text.contains("komprexo.support@gmail.com"))
        }
    }
    @Test fun validFakePlayPriceEnablesPurchaseWithoutUnlockOnLaunch() {
        val transport=FakeBillingTransport();val billing=BillingController(transport,MemoryOwnershipStore(),pause={})
        runBlocking { billing.refresh() }
        main { compose.activity.setContent { PremiumScreen(controller=billing,onBuy={ runBlocking { billing.buy { BillingCode.OK } } },onBack={}) } }
        compose.onNodeWithTag("purchasePremium").assertIsEnabled().performClick()
        assertEquals(EntitlementState.Free,billing.state.value);assertEquals(BillingStatus.PROCESSING,billing.ui.value.status)
        compose.onNodeWithTag("purchasePremium").assertIsNotEnabled()
    }
    private fun large(tag:String,landscape:Boolean) {
        locale(tag)
        if(landscape) {
            orientation(true)
        }
        main { compose.activity.setContent {
            val d=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density,2f)) { KomprexoApp() }
        } }
        compose.waitForIdle()
        try {
            settings();compose.onNodeWithTag("languageSettings").performScrollTo().assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag("legal_premium").performScrollTo().assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag("contactSupport").performScrollTo()
            compose.waitForIdle()
            compose.onNodeWithTag("contactSupport").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        } catch(error:Throwable) {
            runCatching { captureEvidence("phase4-layout-failure-"+tag) }
            throw error
        }
        captureEvidence("phase4-settings-$tag-${if(landscape) "landscape" else "compact"}-font200")
    }
    @Test fun englishCompactFont200() { large("en",false) }
    @Test fun spanishLandscapeFont200() { large("es",true) }
    @Test fun portugueseCompactFont200() { large("pt-BR",false) }
    @Test fun hindiCompactFont200() { large("hi",false) }
    @Test fun legalNavigationReturnsWithoutResettingWork() {
        settings();compose.onNodeWithTag("legal_terms").performScrollTo().performClick()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("languageSettings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("homeNavigation").performClick()
        compose.onNodeWithTag("homecompress").assertExists()
    }
    @Test fun supportContactIsConsistentAcrossLanguages() {
        for(tag in listOf("en","id","es","pt-BR","hi")) {
            val config=Configuration(compose.activity.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(tag)) }
            val context=compose.activity.createConfigurationContext(config)
            assertTrue(context.getString(R.string.contact_support).isNotBlank())
            assertTrue(context.assets.open("legal/$tag/support.txt").bufferedReader().use { it.readText() }.contains("komprexo.support@gmail.com"))
        }
    }
    @Test fun contactSupportUsesOfficialSendToWithoutAttachments() {
        settings()
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_SENDTO)).respondWith(android.app.Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null))
            compose.onNodeWithTag("contactSupport").performScrollTo().performClick()
            intended(allOf(hasAction(Intent.ACTION_SENDTO),hasData(Uri.parse("mailto:komprexo.support@gmail.com"))))
            val sent=Intents.getIntents().last { it.action==Intent.ACTION_SENDTO }
            assertNull(sent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM))
            assertNull(sent.clipData)
        } finally { Intents.release() }
    }
    @Test fun allBillingMessagesAreLocalized() {
        for(tag in listOf("en","id","es","pt-BR","hi")) {
            val config=Configuration(compose.activity.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(tag)) }
            val context=compose.activity.createConfigurationContext(config)
            for(status in BillingStatus.entries) assertTrue(context.getString(billingStatusLabel(status)).isNotBlank())
        }
    }
}
