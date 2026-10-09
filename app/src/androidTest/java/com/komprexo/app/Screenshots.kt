package com.komprexo.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Emulator-only QA evidence, never part of the production app. */
fun captureEvidence(name: String, darkTheme: Boolean? = null) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    // SystemUI renders in a separate process; Compose idleness does not cover it.
    android.os.SystemClock.sleep(350)
    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())

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
        if (darkTheme != null) {
            val density = instrumentation.targetContext.resources.displayMetrics.density
            fun countIcons(start: Int, end: Int, light: Boolean): Int {
                var count = 0
                for (y in start until end) for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    val luma = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)) / 3
                    if (if (light) luma > 200 else luma < 130) count++
                }
                return count
            }
            fun backgroundLuma(start: Int, end: Int): Int {
                val histogram=IntArray(256)
                for(y in start until end) for(x in 0 until bitmap.width) {
                    val pixel=bitmap.getPixel(x,y)
                    histogram[(android.graphics.Color.red(pixel)+android.graphics.Color.green(pixel)+android.graphics.Color.blue(pixel))/3]++
                }
                val midpoint=bitmap.width*(end-start)/2
                var count=0
                for(luma in histogram.indices) { count+=histogram[luma];if(count>midpoint) return luma }
                return 0
            }
            val statusEnd=(24*density).toInt()
            org.junit.Assert.assertTrue("Status background must follow theme dark=$darkTheme",if(darkTheme) backgroundLuma(0,statusEnd)<130 else backgroundLuma(0,statusEnd)>150)
            org.junit.Assert.assertTrue("Visible status icons must contrast with theme dark=$darkTheme", countIcons(0, (24 * density).toInt(), darkTheme) > 100)
            val lightNavigation = darkTheme || android.os.Build.VERSION.SDK_INT < 26
            val navigationStart=bitmap.height-(48*density).toInt()
            org.junit.Assert.assertTrue("Navigation background must contrast with icons",if(lightNavigation) backgroundLuma(navigationStart,bitmap.height)<130 else backgroundLuma(navigationStart,bitmap.height)>150)
            org.junit.Assert.assertTrue("Visible navigation icons must contrast with theme", countIcons(bitmap.height - (48 * density).toInt(), bitmap.height, lightNavigation) > 100)
        }
    } finally { bitmap.recycle() }
}
