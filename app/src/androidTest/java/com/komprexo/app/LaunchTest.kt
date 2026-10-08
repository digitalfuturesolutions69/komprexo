package com.komprexo.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun launchAndRecreateShowsKomprexo() {
        compose.onNodeWithText("Komprexo").assertIsDisplayed()
        compose.onNodeWithText("Select image").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Komprexo").assertIsDisplayed()
        compose.onNodeWithText("Compress image").assertIsNotEnabled()
    }
    @Test fun customSizeRejectsInvalidInput() {
        compose.onNodeWithText("Custom").performClick()
        compose.onNodeWithText("Maximum size in KB").performTextInput("0")
        compose.onNodeWithText("Compress image").assertIsNotEnabled()
    }
}
