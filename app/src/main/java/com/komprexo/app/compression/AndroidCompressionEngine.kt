package com.komprexo.app.compression

import com.komprexo.app.diagnostics.*
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.math.floor

/** Memory-bounded resolution-first search; every trial derives from normalized source pixels. */
class AndroidCompressionEngine(private val cache: File,
    private val traceFactory: () -> DiagnosticTrace = { DiagnosticTrace() },
    private val encoder: BitmapEncodeBackend = platformEncoder,
    private val decoder: (ImageSource, CompressionMode, DiagnosticTrace) -> Bitmap = { source, mode, trace -> BitmapCodec.decode(source, mode = mode, trace = trace) }
) : CompressionEngine {
    companion object { private val memoryGate = Mutex() }
    override suspend fun compress(request: CompressionRequest, progress: (CompressionProgress) -> Unit): CompressionResult {
        val trace = traceFactory().apply { mime(request.source.declaredMime) }
        var ownedDirectory: File? = null
        try { return withContext(Dispatchers.Default) {
        memoryGate.withLock {
            var master: Bitmap? = null
            var scaled: Bitmap? = null
            var directory: File? = null
            var keep = false
            try {
                if (request.maxBytes !in ImageLimits.MIN_TARGET_BYTES..ImageLimits.MAX_TARGET_BYTES) throw ImageProblem(FailureCode.UNREACHABLE_TARGET)
                currentCoroutineContext().ensureActive()
                // Re-inspect to reject stale/tampered input, never trust caller-supplied bounds.
                val source = BitmapCodec.inspect(request.source.file, trace)
                directory = File(cache, "result-${java.util.UUID.randomUUID()}")
                ownedDirectory = directory
                if (!directory.mkdirs()) throw IOException("Cannot create output")
                master = decoder(source, request.options.mode, trace)
                val alpha = master.hasAlpha()
                val formats = if (request.options.mode == CompressionMode.QUALITY_FIRST && request.options.format == OutputFormat.AUTO) {
                    if (alpha) listOf(OutputFormat.PNG, OutputFormat.WEBP) else listOf(OutputFormat.JPEG, OutputFormat.WEBP)
                } else listOf(if (request.options.format == OutputFormat.AUTO) {
                    if (alpha) OutputFormat.WEBP else OutputFormat.JPEG
                } else request.options.format)
                if (alpha && formats.singleOrNull() == OutputFormat.JPEG) {
                    val opaque = BitmapCodec.whiteBackground(master)
                    master.recycle(); master = opaque
                }
                var width = master.width
                var height = master.height
                if (formats.singleOrNull() == OutputFormat.WEBP) {
                    val factor = minOf(1.0, ImageLimits.MAX_WEBP_EDGE.toDouble() / width, ImageLimits.MAX_WEBP_EDGE.toDouble() / height)
                    width = maxOf(1, floor(width * factor).toInt()); height = maxOf(1, floor(height * factor).toInt())
                }
                var attempts = 0
                val qualityFirst = request.options.mode == CompressionMode.QUALITY_FIRST
                while (true) {
                    currentCoroutineContext().ensureActive()
                    trace.move(ProcessingStage.SCALE)
                    scaled = if (width == master.width && height == master.height) master else Bitmap.createScaledBitmap(master, width, height, true)
                    val qualities = if (qualityFirst) (100 downTo 70 step 5).toList() else (100 downTo ImageLimits.MIN_QUALITY).toList()
                    for (quality in qualities) {
                      for (format in formats) {
                        if (format == OutputFormat.PNG && quality != 100) continue
                        if (format == OutputFormat.WEBP && (width > ImageLimits.MAX_WEBP_EDGE || height > ImageLimits.MAX_WEBP_EDGE)) continue
                        val candidate = File(directory, "compressed.${format.extension}")
                        val encodeFormat = when (format) {
                            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                            OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                            else -> Bitmap.CompressFormat.JPEG
                        }
                        trace.encoder = when (format) {
                            OutputFormat.PNG -> CodecName.PNG
                            OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) CodecName.WEBP_LOSSY else CodecName.WEBP_LEGACY
                            else -> CodecName.JPEG
                        }
                        trace.move(ProcessingStage.ENCODE)
                        currentCoroutineContext().ensureActive()
                        progress(CompressionProgress(++attempts, width, height))
                        val job = currentCoroutineContext()
                        val exceeded = candidate.outputStream().use { raw ->
                            val bounded = LimitedOutput(raw, request.maxBytes) { job.isActive }
                            val encoded = encoder.encode(scaled, encodeFormat, quality, bounded)
                            job.ensureActive()
                            bounded.failure?.let { throw it }
                            if (!encoded && !bounded.exceeded) throw ImageProblem(FailureCode.ENCODER_FAILED)
                            bounded.exceeded
                        }
                        val size = candidate.length()
                        if (!exceeded && size in 1..request.maxBytes) {
                            trace.move(ProcessingStage.OUTPUT_VALIDATE)
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(candidate.path, bounds)
                            if (bounds.outWidth != width || bounds.outHeight != height) throw ImageProblem(FailureCode.OUTPUT_INVALID)
                            currentCoroutineContext().ensureActive()
                            trace.move(ProcessingStage.READY)
                            keep = true
                            return@withLock CompressionResult.Success(candidate, source.bytes, size, width, height, format, quality, request.maxBytes, alpha && format == OutputFormat.JPEG)
                        }
                        candidate.delete()
                      }
                    }
                    if (scaled !== master) scaled.recycle()
                    scaled = null
                    if (width == 1 && height == 1) throw ImageProblem(FailureCode.UNREACHABLE_TARGET)
                    // Calculate from the normalized master aspect ratio, never a previous lossy file.
                    val factor = (if (qualityFirst) 0.9 else 0.8) * minOf(width.toDouble() / master.width, height.toDouble() / master.height)
                    width = maxOf(1, floor(master.width * factor).toInt())
                    height = maxOf(1, floor(master.height * factor).toInt())
                }
                CompressionResult.Failed(CompressionFailure(FailureCode.UNREACHABLE_TARGET))
            } catch (e: CancellationException) { trace.record(e); throw e }
            catch (e: OutOfMemoryError) { trace.record(e); CompressionResult.Failed(CompressionFailure(FailureCode.INSUFFICIENT_MEMORY)) }
            catch (e: Exception) { trace.record(e); CompressionResult.Failed(CompressionFailure(classifyFailure(e, trace.stage))) }
            finally {
                if (scaled !== master) scaled?.recycle()
                master?.recycle()
                if (!keep) directory?.deleteRecursively()
            }
        }
        } } catch (e: CancellationException) {
            // Also covers cancellation during the dispatch back to the caller.
            ownedDirectory?.deleteRecursively()
            throw e
        }
    }
}

internal class LimitedOutput(private val sink: OutputStream, private val limit: Long, private val active: () -> Boolean) : OutputStream() {
    private var count = 0L
    var exceeded = false
        private set
    var failure: IOException? = null
        private set
    override fun write(value: Int) {
        if (!active()) { exceeded = true; return }
        if (failure != null) return
        if (exceeded || count >= limit) { exceeded = true; return }
        try { sink.write(value); count++ } catch (e: IOException) { failure = e }
    }
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (!active()) { exceeded = true; return }
        if (failure != null) return
        if (exceeded || length > limit - count) { exceeded = true; return }
        try { sink.write(bytes, offset, length); count += length } catch (e: IOException) { failure = e }
    }
}
fun storageFailure(error: IOException): FailureCode {
    var cause: Throwable? = error
    while (cause != null) {
        if (cause is android.system.ErrnoException && cause.errno == android.system.OsConstants.ENOSPC) return FailureCode.STORAGE_FULL
        if (cause.message?.contains("ENOSPC") == true || cause.message?.contains("No space left") == true) return FailureCode.STORAGE_FULL
        cause = cause.cause
    }
    return FailureCode.FILE_ACCESS
}

fun interface BitmapEncodeBackend { fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, output: OutputStream): Boolean }
private val platformEncoder = BitmapEncodeBackend { bitmap, format, quality, output -> bitmap.compress(format, quality, output) }
