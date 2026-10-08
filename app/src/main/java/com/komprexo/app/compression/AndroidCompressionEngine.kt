package com.komprexo.app.compression

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

/** Resolution-first, exhaustive descending quality search. Each trial uses source pixels. */
class AndroidCompressionEngine(private val cache: File) : CompressionEngine {
    companion object { private val memoryGate = Mutex() }
    override suspend fun compress(request: CompressionRequest, progress: (CompressionProgress) -> Unit): CompressionResult {
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
                val source = BitmapCodec.inspect(request.source.file)
                directory = File(cache, "result-${java.util.UUID.randomUUID()}")
                ownedDirectory = directory
                if (!directory.mkdirs()) throw IOException("Cannot create output")
                master = BitmapCodec.decode(source)
                val alpha = master.hasAlpha()
                val format = if (request.options.format == OutputFormat.AUTO) {
                    if (alpha) OutputFormat.WEBP else OutputFormat.JPEG
                } else request.options.format
                if (alpha && format == OutputFormat.JPEG) {
                    val opaque = BitmapCodec.whiteBackground(master)
                    master.recycle(); master = opaque
                }
                val encodeFormat = when (format) {
                    OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                    OutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }
                var width = master.width
                var height = master.height
                var attempts = 0
                val candidate = File(directory, "compressed.${format.extension}")
                while (true) {
                    currentCoroutineContext().ensureActive()
                    scaled = if (width == master.width && height == master.height) master else Bitmap.createScaledBitmap(master, width, height, true)
                    val qualities = if (format == OutputFormat.PNG) listOf(100) else (100 downTo ImageLimits.MIN_QUALITY).toList()
                    for (quality in qualities) {
                        currentCoroutineContext().ensureActive()
                        progress(CompressionProgress(++attempts, width, height))
                        val job = currentCoroutineContext()
                        val exceeded = candidate.outputStream().use { raw ->
                            val bounded = LimitedOutput(raw, request.maxBytes) { job.isActive }
                            val encoded = scaled.compress(encodeFormat, quality, bounded)
                            job.ensureActive()
                            bounded.failure?.let { throw it }
                            if (!encoded && !bounded.exceeded) throw ImageProblem(FailureCode.CORRUPT_IMAGE)
                            bounded.exceeded
                        }
                        val size = candidate.length()
                        if (!exceeded && size in 1..request.maxBytes) {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(candidate.path, bounds)
                            if (bounds.outWidth != width || bounds.outHeight != height) throw ImageProblem(FailureCode.CORRUPT_IMAGE)
                            currentCoroutineContext().ensureActive()
                            keep = true
                            return@withLock CompressionResult.Success(candidate, source.bytes, size, width, height, format, quality, request.maxBytes, alpha && format == OutputFormat.JPEG)
                        }
                        candidate.delete()
                    }
                    if (scaled !== master) scaled.recycle()
                    scaled = null
                    if (width == 1 && height == 1) throw ImageProblem(FailureCode.UNREACHABLE_TARGET)
                    // Calculate from the normalized master aspect ratio, never a previous lossy file.
                    val factor = 0.8 * minOf(width.toDouble() / master.width, height.toDouble() / master.height)
                    width = maxOf(1, floor(master.width * factor).toInt())
                    height = maxOf(1, floor(master.height * factor).toInt())
                }
                CompressionResult.Failed(CompressionFailure(FailureCode.UNREACHABLE_TARGET))
            } catch (e: CancellationException) { throw e }
            catch (e: ImageProblem) { CompressionResult.Failed(CompressionFailure(e.code)) }
            catch (_: OutOfMemoryError) { CompressionResult.Failed(CompressionFailure(FailureCode.INSUFFICIENT_MEMORY)) }
            catch (e: IOException) { CompressionResult.Failed(CompressionFailure(storageFailure(e))) }
            catch (_: SecurityException) { CompressionResult.Failed(CompressionFailure(FailureCode.FILE_ACCESS)) }
            catch (_: RuntimeException) { CompressionResult.Failed(CompressionFailure(FailureCode.INTERRUPTED)) }
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
