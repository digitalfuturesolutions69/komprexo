package com.komprexo.app

import android.app.Activity
import android.os.Bundle
import android.view.View

/** Native launch shell for Phase 0. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val root = findViewById<View>(R.id.main)
        // Android 15 enforces edge-to-edge; keep content clear of system bars.
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom,
            )
            insets
        }
        root.requestApplyInsets()
    }
}
