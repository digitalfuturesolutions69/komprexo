package com.komprexo.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.komprexo.app.compression.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.log10

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QualityComparisonTest {
    @Test fun compareDetailAgainstBalancedBaseline() = runBlocking {
        val source = BitmapCodec.inspect(File("src/test/assets/detail.png"))
        val reference = BitmapFactory.decodeFile(source.file.path)
        val root = File(RuntimeEnvironment.getApplication().cacheDir, "benchmark").apply { mkdirs() }
        val reports = File("build/reports/quality").apply { mkdirs() }
        val rows = mutableListOf<String>()
        try {
            for (target in listOf(2L * 1024 * 1024, 500L * 1024, 100L * 1024)) {
                var baselinePsnr = 0.0
                for (mode in CompressionMode.entries) {
                    val r = AndroidCompressionEngine(root).compress(CompressionRequest(source, target, CompressionOptions(OutputFormat.AUTO, mode))) as CompressionResult.Success
                    assertTrue(r.meetsTarget)
                    val image = BitmapFactory.decodeFile(r.file.path)
                    val full = Bitmap.createScaledBitmap(image, reference.width, reference.height, true)
                    var error = 0.0
                    var count = 0L
                    for (y in 0 until reference.height step 2) for (x in 0 until reference.width step 2) {
                        val a = reference.getPixel(x, y); val b = full.getPixel(x, y)
                        for (shift in listOf(0, 8, 16)) { val delta = ((a shr shift) and 255) - ((b shr shift) and 255); error += delta * delta; count++ }
                    }
                    val psnr = 10 * log10(255.0 * 255 * count / error)
                    if (mode == CompressionMode.BALANCED) baselinePsnr = psnr
                    else assertTrue("Quality-first should retain more detail in this seeded fixture", psnr > baselinePsnr)
                    val output = File(reports, "${target}-${mode}.${r.format.extension}")
                    r.file.copyTo(output, overwrite = true)
                    rows += "{\"target\":$target,\"mode\":\"$mode\",\"bytes\":${r.bytes},\"width\":${r.width},\"height\":${r.height},\"format\":\"${r.format}\",\"quality\":${r.quality},\"psnrDb\":$psnr}"
                    if (full !== image) full.recycle(); image.recycle()
                }
            }
            File(reports, "comparison.json").writeText("[${rows.joinToString(",\n")}]")
        } finally { reference.recycle(); root.deleteRecursively() }
    }
}
