package com.komprexo.app

import android.view.ViewGroup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityTest {
    @Test fun launchCreatesComposeHostWithKomprexoIdentity() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            val activity = controller.setup().get()
            assertEquals("com.digitalfuturesolutions.komprexo",activity.packageName)
            assertEquals("Komprexo",activity.getString(R.string.app_name))
            assertTrue(activity.findViewById<ViewGroup>(android.R.id.content).childCount > 0)
        }
    }
}
