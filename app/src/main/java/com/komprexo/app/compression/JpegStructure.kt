package com.komprexo.app.compression

import java.io.File

/** Find the main EOI by walking marker lengths and entropy stuffing, not EXIF thumbnails.
 * A main EOI may precede a camera trailer (for example a motion-photo payload).
 */
internal fun hasCompleteJpeg(file: File): Boolean = file.inputStream().buffered(16 * 1024).use { input ->
    if (input.read() != 0xff || input.read() != 0xd8) return@use false
    var scan = false
    var sawScan = false
    while (true) {
        var prefix = input.read()
        if (scan) {
            while (prefix >= 0 && prefix != 0xff) prefix = input.read()
        }
        if (prefix != 0xff) return@use false
        var marker = input.read()
        while (marker == 0xff) marker = input.read()
        if (marker < 0) return@use false
        if (scan && (marker == 0 || marker in 0xd0..0xd7)) continue
        scan = false
        if (marker == 0xd9) return@use sawScan
        if (marker == 0xd8 || marker == 0) return@use false
        if (marker == 1 || marker in 0xd0..0xd7) continue
        val high = input.read(); val low = input.read()
        if (high < 0 || low < 0) return@use false
        var remaining = ((high shl 8) or low) - 2
        if (remaining < 0) return@use false
        while (remaining > 0) {
            val skipped = input.skip(remaining.toLong()).toInt()
            if (skipped > 0) remaining -= skipped
            else { if (input.read() < 0) return@use false; remaining-- }
        }
        if (marker == 0xda) { scan = true; sawScan = true }
    }
    false
}
