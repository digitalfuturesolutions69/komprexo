package com.komprexo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.komprexo.app.ui.KomprexoApp

class MainActivity : ComponentActivity() {
    fun applySystemBars(dark: Boolean) {
        val lightScrim = android.graphics.Color.rgb(245, 245, 245)
        val darkScrim = android.graphics.Color.rgb(27, 27, 27)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(lightScrim, darkScrim) { dark }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KomprexoApp() }
    }
}
