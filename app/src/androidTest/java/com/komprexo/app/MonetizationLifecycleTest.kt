package com.komprexo.app

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.komprexo.app.access.*
import com.komprexo.app.compression.OutputFormat
import com.komprexo.app.ui.CompressionViewModel
import org.junit.*
import org.junit.Assert.*

/** Production application factory and shared real DataStore, including Android recreation. */
class MonetizationLifecycleTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun free() { EntitlementProviderFactory.setTestingPremium(false) }
    @After fun cleanup() { EntitlementProviderFactory.setTestingPremium(false) }
    @Test fun activityRecreationKeepsCommittedQuotaAndExport() {
        compose.onNodeWithTag("homecompress").performScrollTo().performClick()
        lateinit var vm: CompressionViewModel
        compose.activityRule.scenario.onActivity { vm=ViewModelProvider(it)[CompressionViewModel::class.java];vm.select(Uri.parse("content://com.komprexo.app.test.gallery/stream")) }
        compose.waitUntil(30000) { vm.state.value.source!=null && !vm.state.value.busy && vm.quota.state.value.remaining!=null }
        val before=vm.quota.state.value.remaining!!
        compose.activityRule.scenario.onActivity { vm.compress(200*1024,OutputFormat.AUTO) }
        compose.waitUntil(60000) { vm.state.value.result!=null && !vm.state.value.busy }
        assertEquals(before-1,vm.quota.state.value.remaining)
        val source=vm.state.value.source!!.file;val output=vm.state.value.result!!.file
        compose.activityRule.scenario.recreate()
        lateinit var recreated: CompressionViewModel
        compose.activityRule.scenario.onActivity { recreated=ViewModelProvider(it)[CompressionViewModel::class.java] }
        assertSame(vm,recreated);assertEquals(before-1,recreated.quota.state.value.remaining)
        assertTrue(source.exists());assertTrue(output.exists())
        compose.onNodeWithTag("saveAction").assertIsDisplayed();compose.onNodeWithTag("shareAction").assertIsDisplayed()
        compose.onNodeWithTag("quotaIndicator").performScrollTo().assertIsDisplayed()
    }
}
