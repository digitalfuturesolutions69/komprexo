package com.komprexo.app.compression

import com.komprexo.app.diagnostics.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.os.Build
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max

/** Only bounded software ARGB bitmaps; no hardware bitmap or full-size preview. */
object BitmapCodec {
    fun inspect(file: File, trace: DiagnosticTrace = DiagnosticTrace()): ImageSource {
        try {
        trace.move(ProcessingStage.STRUCTURE)
        val bytes = file.length()
        if (bytes <= 0) throw ImageProblem(FailureCode.INVALID_IMAGE)
        if (bytes > ImageLimits.MAX_INPUT_BYTES) throw ImageProblem(FailureCode.INPUT_TOO_LARGE)
        val header = ByteArray(12)
        file.inputStream().use { it.read(header) }
        val mime = when {
            header[0] == 0xff.toByte() && header[1] == 0xd8.toByte() && header[2] == 0xff.toByte() -> "image/jpeg"
            header.take(8).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)) -> "image/png"
            String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
            String(header, 4, 4, Charsets.US_ASCII) == "ftyp" && String(header, 8, 4, Charsets.US_ASCII) in setOf("heic", "heix", "hevc", "hevx", "heif", "mif1", "msf1") -> throw ImageProblem(FailureCode.UNSUPPORTED_HEIF)
            else -> throw ImageProblem(FailureCode.UNSUPPORTED_FORMAT)
        }
        RandomAccessFile(file, "r").use { f ->
            when (mime) {
                "image/jpeg" -> {
                    if (!hasCompleteJpeg(file)) throw ImageProblem(FailureCode.CORRUPT_IMAGE)
                }
                "image/png" -> {
                    if (bytes < 24) throw ImageProblem(FailureCode.CORRUPT_IMAGE)
                    f.seek(bytes - 8)
                    if (f.readInt() != 0x49454e44) throw ImageProblem(FailureCode.CORRUPT_IMAGE)
                }
                "image/webp" -> {
                    val declared = (4..7).foldIndexed(0L) { i, value, n -> value or ((header[n].toLong() and 255) shl (i * 8)) }
                    if (declared + 8 != bytes) throw ImageProblem(FailureCode.CORRUPT_IMAGE)
                }
            }
        }
        trace.decoder = CodecName.BITMAP_FACTORY
        trace.move(ProcessingStage.BOUNDS)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outMimeType != mime) throw ImageProblem(FailureCode.DECODER_FAILED)
        if (bounds.outWidth > ImageLimits.MAX_DIMENSION || bounds.outHeight > ImageLimits.MAX_DIMENSION ||
            bounds.outWidth.toLong() * bounds.outHeight > ImageLimits.MAX_SOURCE_PIXELS) throw ImageProblem(FailureCode.INPUT_TOO_LARGE)
        trace.move(ProcessingStage.METADATA)
        val orientation = try { ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            catch (_: java.io.IOException) { throw ImageProblem(FailureCode.DECODER_FAILED) }
        return ImageSource(file, bytes, bounds.outWidth, bounds.outHeight, mime, orientation)
        } catch (e: OutOfMemoryError) { trace.record(e); throw ImageProblem(FailureCode.INSUFFICIENT_MEMORY, AllocationFailure.OUT_OF_MEMORY) }
        catch (e: Exception) { trace.record(e); throw ImageProblem(classifyFailure(e, trace.stage), (e as? ImageProblem)?.allocation ?: AllocationFailure.NONE) }
    }

    fun sampleSize(width: Int, height: Int, edge: Int = ImageLimits.MAX_DECODE_EDGE, pixels: Long = ImageLimits.MAX_DECODE_PIXELS): Int {
        var sample = 1
        while (max((width + sample - 1) / sample, (height + sample - 1) / sample) > edge ||
            ((width + sample - 1) / sample).toLong() * ((height + sample - 1) / sample) > pixels) sample *= 2
        return sample
    }

    fun decode(source: ImageSource, preview: Boolean = false, mode: CompressionMode = CompressionMode.BALANCED, trace: DiagnosticTrace = DiagnosticTrace(), memory: DecodeMemory? = null, backend: BitmapDecodeBackend = platformDecoder): Bitmap {
        try {
        source.declaredMime?.let(trace::mime)
        trace.decoder = CodecName.BITMAP_FACTORY
        trace.move(ProcessingStage.MEMORY_PLAN)
        val runtime = Runtime.getRuntime()
        val heap = memory?.maxHeap ?: runtime.maxMemory()
        val available = memory?.available ?: (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()))
        // Legacy/preview reserve 16 bytes per pixel; Quality-first reserves 20.
        val safePixels = if (!preview && mode == CompressionMode.QUALITY_FIRST) qualityPixelBudget(heap, available)
            else minOf(if (preview) 400_000L else ImageLimits.MAX_DECODE_PIXELS, minOf(heap / 4, available / 2) / 16)
        if (safePixels < 65_536) throw ImageProblem(FailureCode.INSUFFICIENT_MEMORY, AllocationFailure.HEAP_BUDGET)
        val sample = sampleSize(source.width, source.height, if (preview) 720 else if (mode == CompressionMode.QUALITY_FIRST) ImageLimits.MAX_DIMENSION else ImageLimits.MAX_DECODE_EDGE, safePixels)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888; inScaled = false
            if (Build.VERSION.SDK_INT >= 26) inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        }
        trace.move(ProcessingStage.DECODE)
        val decoded = backend.decode(source.file, options) ?: throw ImageProblem(FailureCode.DECODER_FAILED)
        if (decoded.width.toLong() * decoded.height > safePixels || decoded.allocationByteCount > safePixels * 4) {
            decoded.recycle(); throw ImageProblem(FailureCode.INSUFFICIENT_MEMORY, AllocationFailure.PIXEL_BOUND)
        }
        trace.move(ProcessingStage.NORMALIZE)
        val matrix = Matrix().apply {
            when (source.orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        return try {
            val normalized = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (normalized !== decoded) decoded.recycle()
            normalized
        } catch (t: Throwable) { decoded.recycle(); throw t }
        } catch (e: OutOfMemoryError) { trace.record(e); throw ImageProblem(FailureCode.INSUFFICIENT_MEMORY, AllocationFailure.OUT_OF_MEMORY) }
        catch (e: Exception) { trace.record(e); throw ImageProblem(classifyFailure(e, trace.stage), (e as? ImageProblem)?.allocation ?: AllocationFailure.NONE) }
    }

    /** Reserve half the heap and at least 32 MiB free; 20 bytes/pixel covers copies and encoder headroom. */
    fun qualityPixelBudget(maxHeap: Long, available: Long): Long =
        minOf(16_000_000L, minOf(maxHeap / 2, maxOf(0L, available - 32L * 1024 * 1024) / 2) / 20)

    fun whiteBackground(bitmap: Bitmap): Bitmap {
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
        return opaque
    }
}

/** Injectable platform boundary for fault tests; production always uses BitmapFactory. */
fun interface BitmapDecodeBackend { fun decode(file: File, options: BitmapFactory.Options): Bitmap? }
data class DecodeMemory(val maxHeap: Long, val available: Long)
private val platformDecoder = BitmapDecodeBackend { file, options -> BitmapFactory.decodeFile(file.path, options) }
