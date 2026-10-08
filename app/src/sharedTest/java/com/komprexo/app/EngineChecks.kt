package com.komprexo.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.komprexo.app.compression.*
import com.komprexo.app.storage.ImageStorage
import com.komprexo.app.storage.copyChecked
import kotlinx.coroutines.*
import org.junit.Assert.*
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream

/** Shared checks use device encoders / Robolectric native graphics, never mock compression. */
class EngineChecks(private val context: Context, private val fixture: (String) -> InputStream) {
    private val root = File(context.cacheDir, "check-${java.util.UUID.randomUUID()}").apply { mkdirs() }
    private val engine = AndroidCompressionEngine(root)
    fun close() { root.deleteRecursively() }
    private fun source(name: String): ImageSource {
        val f = File(root, "source-${java.util.UUID.randomUUID()}")
        fixture(name).use { input -> f.outputStream().use { input.copyTo(it) } }
        return BitmapCodec.inspect(f)
    }
    private suspend fun success(name: String, target: Long, format: OutputFormat = OutputFormat.AUTO): CompressionResult.Success {
        val input = source(name)
        val original = input.file.readBytes()
        val result = engine.compress(CompressionRequest(input, target, CompressionOptions(format)))
        assertTrue("Expected success, got $result", result is CompressionResult.Success)
        result as CompressionResult.Success
        assertTrue(result.meetsTarget)
        assertEquals(result.file.length(), result.bytes)
        assertTrue(result.bytes <= target)
        assertArrayEquals(original, input.file.readBytes())
        val decoded = BitmapFactory.decodeFile(result.file.path)
        assertNotNull(decoded)
        assertEquals(result.width, decoded.width)
        assertEquals(result.height, decoded.height)
        decoded.recycle()
        return result
    }
    fun jpeg() = runBlocking { assertEquals(OutputFormat.JPEG, success("noise.jpg", 100 * 1024).format) }
    fun png() = runBlocking { assertEquals(OutputFormat.PNG, success("alpha.png", 200 * 1024, OutputFormat.PNG).format) }
    fun webp() = runBlocking { assertEquals(OutputFormat.WEBP, success("noise.webp", 100 * 1024, OutputFormat.WEBP).format) }
    fun presets() = runBlocking { listOf(100L,200L,300L,500L,1024L,2048L).forEach { success("noise.jpg", it * 1024) } }
    fun highestQuality() = runBlocking {
        val input=source("noise.jpg")
        val r=engine.compress(CompressionRequest(input,100*1024)) as CompressionResult.Success
        assertEquals(640,r.width);assertEquals(480,r.height)
        assertTrue(r.quality < 100)
        val b=BitmapCodec.decode(input)
        val bytes=java.io.ByteArrayOutputStream()
        assertTrue(b.compress(android.graphics.Bitmap.CompressFormat.JPEG,r.quality+1,bytes))
        assertTrue(bytes.size()>r.maxBytes)
        b.recycle()
    }
    fun customTarget() = runBlocking { success("noise.jpg", 17389L); Unit }
    fun aspectRatio() = runBlocking {
        val r = success("noise.jpg", 8 * 1024)
        assertEquals(640.0 / 480, r.width.toDouble() / r.height, 0.035)
        assertTrue(r.width <= 640 && r.height <= 480)
    }
    fun exifRotation() = runBlocking {
        val r = success("rotated.jpg", 100 * 1024)
        assertEquals(80, r.width); assertEquals(120, r.height)
        val bitmap = BitmapFactory.decodeFile(r.file.path)
        assertTrue(Color.red(bitmap.getPixel(40, 20)) > Color.blue(bitmap.getPixel(40, 20)))
        assertTrue(Color.blue(bitmap.getPixel(40, 100)) > Color.red(bitmap.getPixel(40, 100)))
        bitmap.recycle()
        assertTrue(ExifInterface(r.file).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0) in 0..1)
    }
    fun allOrientations() {
        val input = source("orientations.png")
        val corners = listOf(
            listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW),
            listOf(Color.GREEN,Color.RED,Color.YELLOW,Color.BLUE),
            listOf(Color.YELLOW,Color.BLUE,Color.GREEN,Color.RED),
            listOf(Color.BLUE,Color.YELLOW,Color.RED,Color.GREEN),
            listOf(Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW),
            listOf(Color.BLUE,Color.RED,Color.YELLOW,Color.GREEN),
            listOf(Color.YELLOW,Color.GREEN,Color.BLUE,Color.RED),
            listOf(Color.GREEN,Color.YELLOW,Color.RED,Color.BLUE),
        )
        (1..8).forEach { orientation ->
            val b = BitmapCodec.decode(input.copy(orientation = orientation))
            assertEquals(if (orientation in 5..8) 80 else 120, b.width)
            assertEquals(if (orientation in 5..8) 120 else 80, b.height)
            assertEquals(corners[orientation-1],listOf(b.getPixel(10,10),b.getPixel(b.width-11,10),b.getPixel(10,b.height-11),b.getPixel(b.width-11,b.height-11)))
            b.recycle()
        }
    }
    fun transparency() = runBlocking {
        val r = success("alpha.png", 100 * 1024)
        assertEquals(OutputFormat.WEBP, r.format)
        val b = BitmapFactory.decodeFile(r.file.path)
        assertTrue(Color.alpha(b.getPixel(0, 0)) < 10); b.recycle()
        assertFalse(r.transparencyRemoved)
    }
    fun jpegConversion() = runBlocking {
        val r = success("alpha.png", 100 * 1024, OutputFormat.JPEG)
        assertTrue(r.transparencyRemoved)
        val b = BitmapFactory.decodeFile(r.file.path)
        val c = b.getPixel(0, 0)
        assertEquals(255, Color.alpha(c)); assertTrue(Color.red(c) > 240)
        b.recycle()
    }
    fun largeInput() = runBlocking {
        val input = source("large.png")
        val sample = BitmapCodec.sampleSize(input.width,input.height)
        assertTrue(sample >= 4)
        val b = BitmapCodec.decode(input)
        assertTrue(b.width.toLong()*b.height <= ImageLimits.MAX_DECODE_PIXELS)
        assertTrue(b.allocationByteCount <= ImageLimits.MAX_DECODE_PIXELS * 4)
        b.recycle()
        val r = success("large.png", 100 * 1024)
        assertTrue(r.width <= 2048 && r.height <= 2048)
    }
    fun samplingBoundaries() {
        listOf(32768 to 3906, 10000 to 1, 4096 to 2048, 1 to 32768).forEach { (w,h) ->
            val s = BitmapCodec.sampleSize(w,h)
            assertTrue(((w+s-1)/s).toLong()*((h+s-1)/s) <= ImageLimits.MAX_DECODE_PIXELS)
            assertTrue(maxOf((w+s-1)/s,(h+s-1)/s) <= ImageLimits.MAX_DECODE_EDGE)
        }
    }
    fun smallSource() = runBlocking { val r=success("rotated.jpg",2*1024*1024); assertEquals(80,r.width);assertEquals(120,r.height) }
    fun corruption() { try { source("corrupt.jpg");fail("Corrupt JPEG accepted") } catch (e: ImageProblem) { assertEquals(FailureCode.CORRUPT_IMAGE,e.code) } }
    fun unsupported() { try { source("unsupported.gif");fail("GIF accepted") } catch (e: ImageProblem) { assertEquals(FailureCode.UNSUPPORTED_FORMAT,e.code) } }
    fun oversizeBytes() {
        val f=File(root,"too-large")
        java.io.RandomAccessFile(f,"rw").use { it.setLength(ImageLimits.MAX_INPUT_BYTES+1) }
        try { BitmapCodec.inspect(f);fail("Oversized file accepted") } catch(e:ImageProblem) { assertEquals(FailureCode.INPUT_TOO_LARGE,e.code) }
    }
    fun unreachable() = runBlocking {
        val r=engine.compress(CompressionRequest(source("alpha.png"),1,CompressionOptions(OutputFormat.PNG)))
        assertEquals(FailureCode.UNREACHABLE_TARGET,(r as CompressionResult.Failed).failure.code)
        assertFalse(root.listFiles()!!.any { it.name.startsWith("result-") })
    }
    fun cancellation() = runBlocking {
        val input=source("noise.jpg")
        lateinit var job: Job
        job=launch(start=CoroutineStart.LAZY) { engine.compress(CompressionRequest(input,100)) { job.cancel() } }
        job.start();job.join()
        assertTrue(job.isCancelled)
        assertFalse(root.listFiles()!!.any { it.name.startsWith("result-") })
    }
    fun outputIntegrity() = runBlocking {
        val r=success("noise.jpg",100*1024)
        assertEquals("image/jpeg",BitmapCodec.inspect(r.file).mime)
        assertEquals(null,ExifInterface(r.file).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
    }
    fun storageFailure() = runBlocking {
        val blocked=File(root,"blocked").apply { writeText("file, not directory") }
        val r=AndroidCompressionEngine(blocked).compress(CompressionRequest(source("noise.jpg"),100*1024))
        assertEquals(FailureCode.FILE_ACCESS,(r as CompressionResult.Failed).failure.code)
        try {
            copyChecked(byteArrayOf(1,2,3).inputStream(), object:OutputStream(){ override fun write(b:Int){throw IOException("ENOSPC") } })
            fail("Storage failure ignored")
        } catch(e:IOException) { assertEquals(FailureCode.STORAGE_FULL,storageFailure(e)) }
    }
    fun nativeStorageFailure() {
        val bitmap=BitmapCodec.decode(source("noise.jpg"))
        try {
            val output=LimitedOutput(object:OutputStream() {
                override fun write(value:Int) { throw IOException("ENOSPC") }
                override fun write(bytes:ByteArray,offset:Int,length:Int) { throw IOException("ENOSPC") }
            },1024*1024) { true }
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,output)
            assertNotNull(output.failure)
            assertEquals(FailureCode.STORAGE_FULL,storageFailure(output.failure!!))
        } finally { bitmap.recycle() }
    }
    fun concurrency() = runBlocking {
        val source=source("noise.jpg")
        val results=coroutineScope { listOf(async { engine.compress(CompressionRequest(source,100*1024)) },async { engine.compress(CompressionRequest(source,200*1024)) }).awaitAll() }
        val a=results[0] as CompressionResult.Success;val b=results[1] as CompressionResult.Success
        assertNotEquals(a.file.path,b.file.path); assertTrue(a.meetsTarget && b.meetsTarget)
        assertTrue(a.file.exists() && b.file.exists())
    }
    fun saveAndImport() = runBlocking {
        val result = success("noise.jpg", 100 * 1024)
        val destination = File(context.cacheDir, "shared/test-${java.util.UUID.randomUUID()}.jpg")
        destination.parentFile!!.mkdirs(); destination.createNewFile()
        val uri=androidx.core.content.FileProvider.getUriForFile(context,"${context.packageName}.files",destination)
        val storage=ImageStorage(context)
        storage.save(result,uri)
        assertArrayEquals(result.file.readBytes(),destination.readBytes())
        val imported=storage.import(uri)
        assertEquals("image/jpeg",imported.mime)
        assertArrayEquals(result.file.readBytes(),imported.file.readBytes())
        imported.file.delete();destination.delete()
    }
    fun revokedAccess() = runBlocking {
        val result = success("noise.jpg",100*1024)
        try { ImageStorage(context).save(result,android.net.Uri.parse("content://missing.provider/image"));fail("Missing provider accepted") }
        catch (_:java.io.FileNotFoundException) { }
        assertTrue(result.file.exists())
    }
    fun shareSecurity() = runBlocking {
        val result=success("alpha.png",100*1024)
        val storage=ImageStorage(context)
        val intent=storage.shareIntent(result)
        val uri=intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)!!
        assertEquals("content",uri.scheme)
        assertEquals("${context.packageName}.files",uri.authority)
        assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        context.contentResolver.openInputStream(uri)!!.use { assertArrayEquals(result.file.readBytes(),it.readBytes()) }
        try { androidx.core.content.FileProvider.getUriForFile(context,"${context.packageName}.files",result.file);fail("Private result exposed") } catch(_:IllegalArgumentException) { }
        // Delete only the temporary shared copy owned by this check.
        context.contentResolver.delete(uri,null,null)
        assertTrue(result.file.exists())
    }
}
