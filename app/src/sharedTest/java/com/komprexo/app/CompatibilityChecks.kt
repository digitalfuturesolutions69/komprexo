package com.komprexo.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.komprexo.app.compression.*
import com.komprexo.app.diagnostics.*
import com.komprexo.app.storage.ImageStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import java.io.File
import java.io.InputStream
import java.io.IOException

class CompatibilityChecks(private val context: Context, private val fixture: (String) -> InputStream) {
    private val root = File(context.cacheDir, "compat-${java.util.UUID.randomUUID()}").apply { mkdirs() }
    private val events = mutableListOf<DiagnosticEvent>()
    private fun trace() = DiagnosticTrace { events.add(it) }
    fun close() { root.deleteRecursively() }
    private fun source(name: String): ImageSource {
        val file = File(root, name)
        fixture(name).use { input -> file.outputStream().use { input.copyTo(it) } }
        return BitmapCodec.inspect(file)
    }
    private fun uri(case: String) = Uri.parse("content://com.komprexo.app.test.gallery/$case")
    fun galleryStreaming() = runBlocking {
        for (case in listOf("stream", "offset", "misdeclared", "metadata-fails", "png", "webp")) {
            val storage = ImageStorage(context, ::trace)
            val input = try { storage.import(uri(case)) } catch (e:ImageProblem) { throw AssertionError("Fixture $case: ${e.code}; ${events.last().stage}") }
            try {
                val original = input.file.readBytes()
                val result = AndroidCompressionEngine(root).compress(CompressionRequest(input, 100*1024, CompressionOptions(OutputFormat.WEBP, CompressionMode.QUALITY_FIRST))) as CompressionResult.Success
                assertEquals(OutputFormat.WEBP, result.format); assertTrue(result.meetsTarget)
                assertArrayEquals(original, input.file.readBytes())
                val expected = when (case) { "png" -> "alpha.png"; "webp" -> "noise.webp"; else -> "noise.jpg" }
                assertArrayEquals(fixture(expected).use { it.readBytes() }, original)
            } finally { input.file.delete() }
        }
    }
    fun providerFailures() = runBlocking {
        for ((case, expected) in listOf("revoked" to FailureCode.READ_PERMISSION, "missing" to FailureCode.READ_FAILED, "heif" to FailureCode.UNSUPPORTED_HEIF)) {
            val storage = ImageStorage(context, ::trace)
            val before = storage.cache.listFiles()!!.map { it.name }.toSet()
            try { storage.import(uri(case)); fail("Expected $case rejection") }
            catch (e: ImageProblem) { assertEquals(expected, e.code) }
            assertEquals(before, storage.cache.listFiles()!!.map { it.name }.toSet())
            assertTrue(events.any { it.exception != ExceptionCategory.NONE })
        }
    }
    fun invalidUri() = runBlocking {
        try { ImageStorage(context, ::trace).import(Uri.parse("file:///private/photo.jpg")); fail("Invalid URI accepted") }
        catch (e: ImageProblem) { assertEquals(FailureCode.INVALID_URI, e.code) }
    }
    fun jpegTrailer() = runBlocking {
        val input = source("noise.jpg")
        input.file.appendBytes(byteArrayOf(0,1,2,3))
        val native = BitmapFactory.decodeFile(input.file.path); assertNotNull(native); native.recycle()
        val inspected = BitmapCodec.inspect(input.file)
        val original = input.file.readBytes()
        val result = AndroidCompressionEngine(root).compress(CompressionRequest(inspected,100*1024,CompressionOptions(OutputFormat.WEBP,CompressionMode.QUALITY_FIRST))) as CompressionResult.Success
        assertTrue(result.meetsTarget); assertEquals(OutputFormat.WEBP,result.format)
        assertArrayEquals(original,input.file.readBytes())
    }
    fun jpegThumbnailCannotHideTruncation() {
        val input = source("noise.jpg")
        val original = input.file.readBytes()
        input.file.writeBytes(original.take(2).toByteArray() + byteArrayOf(0xff.toByte(),0xe1.toByte(),0,4,0xff.toByte(),0xd9.toByte()) + original.drop(2).dropLast(2).toByteArray())
        try { BitmapCodec.inspect(input.file); fail("Thumbnail EOI accepted as main EOI") }
        catch (e:ImageProblem) { assertEquals(FailureCode.CORRUPT_IMAGE,e.code) }
    }
    fun webpDimensions() = runBlocking {
        val bitmap = Bitmap.createBitmap(20000,40,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN);setHasAlpha(false) }
        val file = File(root,"wide.png")
        try { file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } }
        finally { bitmap.recycle() }
        val result = AndroidCompressionEngine(root).compress(CompressionRequest(BitmapCodec.inspect(file),100*1024,CompressionOptions(OutputFormat.WEBP,CompressionMode.QUALITY_FIRST))) as CompressionResult.Success
        assertEquals(OutputFormat.WEBP,result.format);assertTrue(result.width<=ImageLimits.MAX_WEBP_EDGE);assertTrue(result.meetsTarget)
        val decoded = BitmapFactory.decodeFile(result.file.path);assertNotNull(decoded);assertEquals(result.width,decoded.width);decoded.recycle()
    }
    fun decoderFailures() {
        val input = source("noise.jpg")
        for (backend in listOf(BitmapDecodeBackend { _, _ -> null }, BitmapDecodeBackend { _, _ -> throw IllegalArgumentException("private decoder details") })) {
            try { BitmapCodec.decode(input,trace=trace(),backend=backend);fail("Decoder failure accepted") }
            catch (e:ImageProblem) { assertEquals(FailureCode.DECODER_FAILED,e.code) }
            assertEquals(ProcessingStage.DECODE,events.last().stage)
        }
    }
    fun encoderFailures() = runBlocking {
        for ((backend, expected) in listOf(
            BitmapEncodeBackend { _,_,_,_ -> false } to FailureCode.ENCODER_FAILED,
            BitmapEncodeBackend { _,_,_,_ -> throw IllegalStateException("private encoder details") } to FailureCode.ENCODER_FAILED,
            BitmapEncodeBackend { _,_,_,_ -> throw OutOfMemoryError("private allocator details") } to FailureCode.INSUFFICIENT_MEMORY)) {
            val result = AndroidCompressionEngine(root,::trace,backend).compress(CompressionRequest(source("noise.jpg"),100*1024,CompressionOptions(OutputFormat.WEBP))) as CompressionResult.Failed
            assertEquals(expected,result.failure.code)
            assertEquals(ProcessingStage.ENCODE,events.last().stage)
            assertFalse(root.listFiles()!!.any { it.name.startsWith("result-") })
        }
    }
    fun memoryPressure() {
        val input = source("noise.jpg")
        for (mode in CompressionMode.entries) {
            try { BitmapCodec.decode(input,mode=mode,trace=trace(),memory=DecodeMemory(64L*1024*1024,1L*1024*1024));fail("Low budget accepted") }
            catch (e:ImageProblem) { assertEquals(FailureCode.INSUFFICIENT_MEMORY,e.code);assertEquals(AllocationFailure.HEAP_BUDGET,e.allocation) }
        }
        try { BitmapCodec.decode(input,trace=trace(),backend=BitmapDecodeBackend { _,_ -> throw OutOfMemoryError("private allocator details") });fail("OOM accepted") }
        catch (e:ImageProblem) { assertEquals(FailureCode.INSUFFICIENT_MEMORY,e.code);assertEquals(AllocationFailure.OUT_OF_MEMORY,e.allocation) }
        assertTrue(events.any { it.allocation==AllocationFailure.OUT_OF_MEMORY })
        val oversized = Bitmap.createBitmap(321,257,Bitmap.Config.ARGB_8888)
        try { BitmapCodec.decode(input,trace=trace(),memory=DecodeMemory(16L*1024*1024,2L*1024*1024),backend=BitmapDecodeBackend { _,_ -> oversized });fail("Decoder ignored allocation bound") }
        catch (e:ImageProblem) { assertEquals(FailureCode.INSUFFICIENT_MEMORY,e.code);assertEquals(AllocationFailure.PIXEL_BOUND,e.allocation) }
        assertTrue(oversized.isRecycled)

    }
    fun errorClassification() {
        assertEquals(FailureCode.READ_PERMISSION,classifyFailure(SecurityException(),ProcessingStage.URI_OPEN))
        assertEquals(FailureCode.READ_FAILED,classifyFailure(IOException(),ProcessingStage.URI_READ))
        assertEquals(FailureCode.DECODER_FAILED,classifyFailure(IllegalArgumentException(),ProcessingStage.DECODE))
        assertEquals(FailureCode.ENCODER_FAILED,classifyFailure(IllegalStateException(),ProcessingStage.ENCODE))
        assertEquals(FailureCode.OUTPUT_INVALID,classifyFailure(IllegalStateException(),ProcessingStage.OUTPUT_VALIDATE))
        assertEquals(FailureCode.INSUFFICIENT_MEMORY,classifyFailure(OutOfMemoryError(),ProcessingStage.ENCODE))
    }
    fun diagnosticsPrivacy() {
        val trace=trace();trace.mime("image/jpeg; filename=private-secret");trace.move(ProcessingStage.DECODE)
        trace.record(IllegalArgumentException("/private/photos/person.jpg EXIF GPS secret-token"))
        val json=events.last().json()
        listOf("private", "person", "GPS", "secret", "/photos", "IllegalArgumentException").forEach { assertFalse(json.contains(it)) }
        assertEquals("other",events.last().declaredMime)
        assertEquals(CodecName.NONE,events.last().decoder)
        assertTrue(json.contains("manufacturer"));assertTrue(json.contains("allocation"))
    }
}
