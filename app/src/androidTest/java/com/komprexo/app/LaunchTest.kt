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
    @Test fun customSizeRejectsInvalidInput() {
        compose.onNodeWithText("Custom").performScrollTo().performClick()
        compose.onNodeWithText("Maximum size in KB").performTextInput("0")
        compose.onNodeWithText("Compress image").assertIsNotEnabled()
    }
}
