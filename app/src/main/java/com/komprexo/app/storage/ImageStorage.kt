package com.komprexo.app.storage

import com.komprexo.app.diagnostics.*
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.komprexo.app.compression.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ImageStorage(internal val context: Context, private val traceFactory: () -> DiagnosticTrace = { DiagnosticTrace() }) {
    val cache = File(context.cacheDir, "images").apply { mkdirs() }
    private val shared = File(context.cacheDir, "shared").apply { mkdirs() }
    init {
        // Never touch external saved files. Shared grants remain usable for 24 hours.
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        listOf(cache, shared).forEach { root -> root.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.deleteRecursively() } }
    }
    suspend fun import(uri: Uri, validatePreview: Boolean = true): ImageSource {
        val trace = traceFactory()
        val file = File(cache, "source-${UUID.randomUUID()}")
        try { return withContext(Dispatchers.IO) {
        trace.move(ProcessingStage.URI_OPEN)
        if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority.isNullOrEmpty()) throw ImageProblem(FailureCode.INVALID_URI)
        var keep = false
        try {
            trace.move(ProcessingStage.MIME_QUERY)
            try { trace.mime(context.contentResolver.getType(uri)) }
            catch (e: RuntimeException) { trace.record(e) } // Optional provider metadata must not gate stream access.
            trace.move(ProcessingStage.URI_OPEN)
            context.contentResolver.openInputStream(uri)?.use { input ->
                trace.move(ProcessingStage.URI_READ)
                file.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > ImageLimits.MAX_INPUT_BYTES) throw ImageProblem(FailureCode.INPUT_TOO_LARGE)
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw ImageProblem(FailureCode.READ_FAILED)
            currentCoroutineContext().ensureActive()
            val source = BitmapCodec.inspect(file, trace).copy(declaredMime = trace.declaredMime)
            // Decode a sampled preview once to validate decodability before accepting selection.
            if (validatePreview) BitmapCodec.decode(source, preview = true, trace = trace).recycle()
            trace.move(ProcessingStage.READY)
            keep = true
            source
        } finally { if (!keep) file.delete() }
        } } catch (e: CancellationException) { file.delete(); trace.record(e); throw e }
        catch (e: OutOfMemoryError) { file.delete(); trace.record(e); throw ImageProblem(FailureCode.INSUFFICIENT_MEMORY, AllocationFailure.OUT_OF_MEMORY) }
        catch (e: Exception) { file.delete(); trace.record(e); throw ImageProblem(classifyFailure(e, trace.stage)) }
    }

    suspend fun save(result: CompressionResult.Success, destination: Uri) = withContext(Dispatchers.IO) {
        if (destination.scheme != ContentResolver.SCHEME_CONTENT) throw ImageProblem(FailureCode.FILE_ACCESS)
        context.contentResolver.openOutputStream(destination, "w")?.use { output ->
            result.file.inputStream().use { input -> copyChecked(input, output) }
        } ?: throw ImageProblem(FailureCode.FILE_ACCESS)
    }
    suspend fun shareIntent(result: CompressionResult.Success): Intent = withContext(Dispatchers.IO) {
        // Keep provider scope separate from originals/results. Bound shared disk use.
        if ((shared.listFiles()?.sumOf { it.length() } ?: 0L) + result.bytes > 128L * 1024 * 1024) throw ImageProblem(FailureCode.STORAGE_FULL)
        val file = File(shared, "Komprexo-${UUID.randomUUID()}.${result.format.extension}")
        try {
            file.outputStream().use { output -> result.file.inputStream().use { copyChecked(it, output) } }
            currentCoroutineContext().ensureActive()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            Intent(Intent.ACTION_SEND).apply {
                type = result.format.mime
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("Komprexo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (t: Throwable) { file.delete(); throw t }
    }
    fun deleteResult(result: CompressionResult.Success?) { result?.file?.parentFile?.deleteRecursively() }
}

suspend fun copyChecked(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(16 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        output.write(buffer, 0, count)
    }
    output.flush()
}
