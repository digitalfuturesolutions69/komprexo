package com.komprexo.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.komprexo.app.billing.BillingServices
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.komprexo.app.ui.KomprexoApp

class MainActivity : AppCompatActivity() {
    fun applySystemBars(dark: Boolean) {
        val lightScrim = android.graphics.Color.rgb(245, 245, 245)
        val darkScrim = android.graphics.Color.rgb(27, 27, 27)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(lightScrim, darkScrim) { dark }
        )
    }

    override fun onResume() {
        super.onResume()
        BillingServices.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillingServices.initialize(applicationContext)
        enableEdgeToEdge()
        setContent { KomprexoApp() }
    }
}
