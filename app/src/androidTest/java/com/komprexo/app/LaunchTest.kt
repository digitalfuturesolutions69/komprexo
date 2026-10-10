package com.komprexo.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @org.junit.Before fun enableRegressionPremium() { com.komprexo.app.access.EntitlementProviderFactory.setTestingPremium(true) }
    @org.junit.After fun disableRegressionPremium() { com.komprexo.app.access.EntitlementProviderFactory.setTestingPremium(false) }

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun openAcceptedCompressionWorkflow() { compose.onNodeWithTag("homecompress").performClick() }
    @Test fun launchAndRecreateShowsKomprexo() {
        compose.onNodeWithText("Komprexo").assertIsDisplayed()
        compose.onNodeWithText("Select image").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Komprexo").assertIsDisplayed()
        compose.onNodeWithText("Compress image").assertIsNotEnabled()
    }
    @Test fun approvedApplicationIdentityAndPrivateShareProvider() {
        val context = compose.activity
        val approvedId = "com.digitalfuturesolutions.komprexo"
        org.junit.Assert.assertEquals(approvedId, context.packageName)
        org.junit.Assert.assertEquals(approvedId, BuildConfig.APPLICATION_ID)
        val provider = context.packageManager.resolveContentProvider("$approvedId.files", 0)
        org.junit.Assert.assertNotNull(provider)
        org.junit.Assert.assertEquals(approvedId, provider!!.packageName)
        org.junit.Assert.assertFalse(provider.exported)
        org.junit.Assert.assertTrue(provider.grantUriPermissions)
        val permission = context.packageManager.getPermissionInfo("$approvedId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION", 0)
        org.junit.Assert.assertEquals(approvedId, permission.packageName)
        org.junit.Assert.assertEquals(android.content.pm.PermissionInfo.PROTECTION_SIGNATURE,
            permission.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_MASK_BASE)
    }
    @Test fun customSizeRejectsInvalidInput() {
        compose.onNodeWithText("Custom").performScrollTo().performClick()
        compose.onNodeWithText("Maximum size in KB").performTextInput("0")
        compose.onNodeWithText("Compress image").assertIsNotEnabled()
    }
}
