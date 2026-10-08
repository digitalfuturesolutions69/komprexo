package com.komprexo.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowInsetsControllerCompat
import com.komprexo.app.ui.KomprexoScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun customFieldIsConditionalAndHasTouchTarget() {
        compose.onNodeWithTag("customSize").assertDoesNotExist()
        compose.onNodeWithText("Custom").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("customSize").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("200 KB").performScrollTo().performClick()
        compose.onNodeWithTag("customSize").assertDoesNotExist()
    }
    @Test fun compactLargeFontKeepsPrimaryControlsAccessible() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) { KomprexoScreen() }
            }
        }
        compose.onNodeWithText("Select image").assertIsDisplayed()
        compose.onNodeWithText("Compress image").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        captureEvidence("compact-large-font")
        compose.onNodeWithText("Custom").performScrollTo().performClick()
        compose.onNodeWithTag("customSize").performScrollTo().performTextInput("750")
        compose.onNodeWithText("Quality first").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Auto · recommended").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Compress image").assertIsDisplayed()
    }
    @Test fun lightAndDarkSystemBarsMatchTheme() {
        for (dark in listOf(false, true)) {
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    val configuration = Configuration(LocalConfiguration.current).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    }
                    CompositionLocalProvider(LocalConfiguration provides configuration) { KomprexoScreen() }
                }
            }
            compose.waitForIdle()
            captureEvidence(if (dark) "dark-theme" else "light-theme", darkTheme = dark)
            compose.activityRule.scenario.onActivity { activity ->
                val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                assertEquals(!dark, controller.isAppearanceLightStatusBars)
                if (Build.VERSION.SDK_INT >= 26) assertEquals(!dark, controller.isAppearanceLightNavigationBars)
                else assertTrue("API23 requires a dark navigation background for light icons", Color.red(activity.window.navigationBarColor) < 128)
            }
        }
    }
}
