package com.komprexo.app.processing

import android.graphics.Bitmap
import android.os.Build
import com.komprexo.app.compression.*
import com.komprexo.app.diagnostics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.UUID

/** Exact requested dimensions or an explicit device-limit failure. No file-size search.
 * Shares the compression engine's process-wide native allocation gate. */
class ImageTransformEngine(private val cache: File,
    private val encoder: BitmapEncodeBackend = BitmapEncodeBackend { bitmap,format,quality,stream -> bitmap.compress(format,quality,stream) },
    private val decoder: (ImageSource,DiagnosticTrace) -> Bitmap = { s,t -> BitmapCodec.decode(s,mode=CompressionMode.QUALITY_FIRST,trace=t) }
) : TransformEngine {
    override suspend fun transform(request: TransformRequest): ProcessingResult {
        val trace = DiagnosticTrace().apply { mime(request.source.declaredMime) }
        var owned: File? = null
        try { return withContext(Dispatchers.Default) {
            AndroidCompressionEngine.memoryGate.withLock {
                var master: Bitmap? = null
                var resized: Bitmap? = null
                var keep = false
                try {
                    ensureActive()
                    val source = BitmapCodec.inspect(request.source.file,trace)
                    val size = request.resize.resolve(source.visualDimensions())
                    val format = if (request.format == OutputFormat.AUTO) when(source.mime) {
                        "image/png" -> OutputFormat.PNG
                        "image/webp" -> OutputFormat.WEBP
                        else -> OutputFormat.JPEG
                    } else request.format
                    if (format == OutputFormat.WEBP && maxOf(size.width,size.height) > ImageLimits.MAX_WEBP_EDGE) throw ImageProblem(FailureCode.DEVICE_LIMIT)
                    master = decoder(source,trace)
                    if (size.width > master.width || size.height > master.height) throw ImageProblem(FailureCode.DEVICE_LIMIT)
                    val alphaRemoved = master.hasAlpha() && format == OutputFormat.JPEG
                    if (alphaRemoved && !request.allowAlphaRemoval) throw ImageProblem(FailureCode.ALPHA_CONFIRMATION)
                    if (alphaRemoved) {
                        val opaque = BitmapCodec.whiteBackground(master); master.recycle(); master=opaque
                    }
                    trace.move(ProcessingStage.SCALE)
                    resized = if (size.width == master.width && size.height == master.height) master
                        else Bitmap.createScaledBitmap(master,size.width,size.height,true)
                    val directory = File(cache,"transform-${UUID.randomUUID()}").also { owned=it }
                    if (!directory.mkdirs()) throw IOException("Cannot create output")
                    val file = File(directory,"image.${format.extension}")
                    val encoder = when(format) {
                        OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                        OutputFormat.WEBP -> if(Build.VERSION.SDK_INT>=30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                        else -> Bitmap.CompressFormat.JPEG
                    }
                    trace.encoder = when(format) {
                        OutputFormat.PNG -> CodecName.PNG
                        OutputFormat.WEBP -> if(Build.VERSION.SDK_INT>=30) CodecName.WEBP_LOSSY else CodecName.WEBP_LEGACY
                        else -> CodecName.JPEG
                    }
                    trace.move(ProcessingStage.ENCODE)
                    val job = currentCoroutineContext()
                    file.outputStream().use { stream ->
                        val bounded = LimitedOutput(stream,MAX_TRANSFORM_BYTES) { job.isActive }
                        val encoded = this@ImageTransformEngine.encoder.encode(resized,encoder,if(format==OutputFormat.PNG) 100 else 95,bounded)
                        job.ensureActive(); bounded.failure?.let { throw it }
                        if (bounded.exceeded) throw ImageProblem(FailureCode.OUTPUT_TOO_LARGE)
                        if (!encoded) throw ImageProblem(FailureCode.ENCODER_FAILED)
                    }
                    trace.move(ProcessingStage.OUTPUT_VALIDATE)
                    try {
                        val inspected = BitmapCodec.inspect(file,trace)
                        if (inspected.width != size.width || inspected.height != size.height || inspected.mime != format.mime) throw ImageProblem(FailureCode.OUTPUT_INVALID)
                        // Real sampled decode, not just header parsing.
                        BitmapCodec.decode(inspected,preview=true,trace=trace).recycle()
                    } catch(e: ImageProblem) {
                        if(e.code in setOf(FailureCode.INSUFFICIENT_MEMORY,FailureCode.CANCELLED,FailureCode.FILE_ACCESS,FailureCode.STORAGE_FULL)) throw e
                        trace.move(ProcessingStage.OUTPUT_VALIDATE)
                        throw ImageProblem(FailureCode.OUTPUT_INVALID)
                    }
                    ensureActive(); trace.move(ProcessingStage.READY)
                    keep=true
                    ProcessingResult.Success(ImageOutput(file,source.bytes,file.length(),size.width,size.height,format,alphaRemoved))
                } catch (e: CancellationException) { trace.record(e); throw e }
                  catch (e: OutOfMemoryError) { trace.record(e); ProcessingResult.Failed(FailureCode.INSUFFICIENT_MEMORY) }
                  catch (e: Exception) { trace.record(e); ProcessingResult.Failed(classifyFailure(e,trace.stage)) }
                finally {
                    if (resized !== master) resized?.recycle()
                    master?.recycle()
                    if (!keep) owned?.deleteRecursively()
                }
            }
        } } catch (e: CancellationException) { owned?.deleteRecursively(); throw e }
    }
}
