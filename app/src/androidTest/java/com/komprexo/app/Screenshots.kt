package com.komprexo.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Emulator-only QA evidence, never part of the production app. */
fun captureEvidence(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
    try {
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val file = File(directory, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // AGP uninstalls the app after testing; shell-owned evidence survives cleanup.
        for (command in listOf("mkdir -p /data/local/tmp/komprexo-evidence", "cp ${file.absolutePath} /data/local/tmp/komprexo-evidence/$name.png")) {
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        }
    } finally { bitmap.recycle() }
}
