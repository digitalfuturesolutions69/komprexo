package com.komprexo.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.DocumentsContract
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import com.komprexo.app.storage.*
import com.komprexo.app.ui.EditableSettings
import kotlinx.coroutines.*
import org.junit.Assert.*
import java.io.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class Phase2Checks(private val context: Context, private val fixture: (String)->InputStream) {
    private val root=File(context.cacheDir,"phase2-check-${UUID.randomUUID()}").apply { mkdirs() }
    private val transform=ImageTransformEngine(root)
    private val engine=AndroidCompressionEngine(root)
    fun close() { root.deleteRecursively() }
    private fun source(name: String): ImageSource {
        val file=File(root,"source-${UUID.randomUUID()}")
        fixture(name).use { input->file.outputStream().use { input.copyTo(it) } }
        return BitmapCodec.inspect(file)
    }
    private suspend fun success(name: String, format: OutputFormat, resize: ResizeSpec=ResizeSpec.Original, alpha: Boolean=false): ImageOutput {
        val source=source(name);val original=source.file.readBytes()
        val result=transform.transform(TransformRequest(source,format,resize,alpha))
        assertTrue("$result",result is ProcessingResult.Success)
        val output=(result as ProcessingResult.Success).output
        assertEquals(output.bytes,output.file.length());assertNull(output.meetsTarget)
        assertArrayEquals(original,source.file.readBytes())
        val decoded=BitmapFactory.decodeFile(output.file.path);assertNotNull(decoded)
        assertEquals(output.width,decoded.width);assertEquals(output.height,decoded.height);decoded.recycle()
        assertEquals(format.mime,BitmapCodec.inspect(output.file).mime)
        assertEquals(format.extension,output.file.extension)
        return output
    }
    fun jpegPng()=runBlocking { success("noise.jpg",OutputFormat.PNG) }
    fun pngJpeg()=runBlocking { assertTrue(success("alpha.png",OutputFormat.JPEG,alpha=true).transparencyRemoved) }
    fun jpegWebp()=runBlocking { success("noise.jpg",OutputFormat.WEBP) }
    fun webpJpeg()=runBlocking { success("noise.webp",OutputFormat.JPEG) }
    fun pngTransparency()=runBlocking {
        for(format in listOf(OutputFormat.PNG,OutputFormat.WEBP)) {
            val output=success("alpha.png",format)
            val decoded=BitmapFactory.decodeFile(output.file.path);assertEquals(0,Color.alpha(decoded.getPixel(0,0)));decoded.recycle()
        }
        val result=transform.transform(TransformRequest(source("alpha.png"),OutputFormat.JPEG))
        assertEquals(FailureCode.ALPHA_CONFIRMATION,(result as ProcessingResult.Failed).code)
    }
    fun unsupportedAndCorrupt()=runBlocking {
        for((name,expected) in listOf("unsupported.gif" to FailureCode.UNSUPPORTED_FORMAT,"corrupt.jpg" to FailureCode.CORRUPT_IMAGE)) {
            val file=File(root,"invalid-${UUID.randomUUID()}");fixture(name).use { input->file.outputStream().use { input.copyTo(it) } }
            val result=transform.transform(TransformRequest(ImageSource(file,file.length(),1,1,"image/jpeg",1),OutputFormat.PNG))
            assertEquals(expected,(result as ProcessingResult.Failed).code)
        }
        assertFalse(root.listFiles()!!.any { it.name.startsWith("transform-") })
    }
    fun percentage()=runBlocking {
        val input=source("noise.jpg")
        val result=transform.transform(TransformRequest(input,OutputFormat.JPEG,ResizeSpec.Percent(50))) as ProcessingResult.Success
        assertEquals(input.width/2,result.output.width);assertEquals(input.height/2,result.output.height);assertNull(result.output.targetBytes)
    }
    fun fixedDimensions()=runBlocking {
        val output=success("noise.jpg",OutputFormat.PNG,ResizeSpec.Custom(120,80,false))
        assertEquals(120,output.width);assertEquals(80,output.height)
    }
    fun aspectRatio() {
        val original=Dimensions(400,300)
        assertEquals(Dimensions(200,150),ResizeSpec.Custom(200,null).resolve(original))
        assertEquals(Dimensions(200,150),ResizeSpec.Custom(null,150).resolve(original))
        assertEquals(Dimensions(200,150),ResizeSpec.Custom(200,200).resolve(original))
        assertEquals(Dimensions(400,300),ResizeSpec.Fit(1600,1600).resolve(original))
        assertEquals(Dimensions(1,1),ResizeSpec.Percent(25).resolve(Dimensions(1,1)))
    }
    fun invalidDimensions() {
        val source=Dimensions(400,300)
        for(spec in listOf(ResizeSpec.Percent(0),ResizeSpec.Percent(101),ResizeSpec.Custom(null,null),ResizeSpec.Custom(0,1),ResizeSpec.Custom(-1,null),ResizeSpec.Custom(500,null),ResizeSpec.Fit(40000,40000))) {
            try { spec.resolve(source);fail("Invalid resize accepted") } catch(e: ImageProblem) { assertEquals(FailureCode.INVALID_DIMENSIONS,e.code) }
        }
    }
    fun orientation()=runBlocking {
        val source=source("rotated.jpg")
        val output=(transform.transform(TransformRequest(source,OutputFormat.PNG)) as ProcessingResult.Success).output
        assertEquals(source.visualDimensions(),Dimensions(output.width,output.height))
    }
    fun memoryAndOversize()=runBlocking {
        val input=source("noise.jpg")
        val sampled=ImageTransformEngine(root) { _,_ -> android.graphics.Bitmap.createBitmap(1,1,android.graphics.Bitmap.Config.ARGB_8888) }
        assertEquals(FailureCode.DEVICE_LIMIT,(sampled.transform(TransformRequest(input,OutputFormat.PNG)) as ProcessingResult.Failed).code)
        val oom=ImageTransformEngine(root) { _,_ -> throw OutOfMemoryError("synthetic allocator") }
        assertEquals(FailureCode.INSUFFICIENT_MEMORY,(oom.transform(TransformRequest(input,OutputFormat.PNG)) as ProcessingResult.Failed).code)
        val invalid=File(root,"oversized").apply { RandomAccessFile(this,"rw").use { it.setLength(ImageLimits.MAX_INPUT_BYTES+1) } }
        assertEquals(FailureCode.INPUT_TOO_LARGE,(transform.transform(TransformRequest(ImageSource(invalid,invalid.length(),1,1,"image/jpeg",1),OutputFormat.PNG)) as ProcessingResult.Failed).code)
        assertFalse(root.listFiles()!!.any { it.name.startsWith("transform-") })
    }
    fun presets()=runBlocking {
        val input=source("noise.jpg")
        for(preset in Preset.entries) {
            val defaults=preset.defaults();assertEquals(defaults,preset.defaults())
            val settings=EditableSettings().apply(preset).copy(targetKiB="100",format=OutputFormat.JPEG)
            assertEquals(100L*1024,settings.options().targetBytes);assertEquals(OutputFormat.JPEG,settings.options().format)
            assertEquals(defaults,preset.defaults())
            val result=engine.compress(CompressionRequest(input,defaults.targetBytes,CompressionOptions(defaults.format,defaults.mode,defaults.resize))) as CompressionResult.Success
            assertTrue(result.meetsTarget)
        }
    }
    fun batchSequential()=runBlocking {
        val input=source("noise.jpg");val original=input.file.readBytes()
        val updates=mutableListOf<BatchProgress>()
        val result=BatchOrchestrator(engine).process(List(3) { BatchInput("$it",input) },Preset.CUSTOM.defaults(),updates::add)
        assertEquals(3,result.completed);assertEquals(0,result.failed);assertEquals(100,result.percent)
        assertEquals(listOf(1,2,3),updates.mapNotNull { it.current })
        assertTrue(updates.zipWithNext().all { (a,b)->a.percent<=b.percent })
        val outputs=result.items.map { (it.result as ItemResult.Success).output }
        assertEquals(3,outputs.map { it.file.canonicalPath }.toSet().size)
        assertArrayEquals(original,input.file.readBytes())
    }
    fun partialBatch()=runBlocking {
        val input=source("noise.jpg")
        val inputs=listOf(BatchInput("valid",input),BatchInput("invalid",null,FailureCode.UNSUPPORTED_FORMAT),BatchInput("again",input))
        val result=BatchOrchestrator(engine).process(inputs,Preset.CUSTOM.defaults()) { }
        assertEquals(2,result.completed);assertEquals(1,result.failed);assertEquals(100,result.percent)
        assertEquals(FailureCode.UNSUPPORTED_FORMAT,(result.items[1].result as ItemResult.Failed).code)
    }
    fun batchCancellation()=runBlocking {
        val input=source("noise.jpg");var progress: BatchProgress?=null;var once=false
        try { BatchOrchestrator(engine).process(List(3) { BatchInput("$it",input) },Preset.CUSTOM.defaults()) {
            progress=it
            if(it.completed==1 && !once) { once=true;throw CancellationException() }
        };fail("Cancel not propagated") } catch(_: CancellationException) { }
        assertEquals(1,progress!!.completed);assertEquals(33,progress!!.percent)
        assertEquals(2,progress!!.items.count { it.result==ItemResult.Cancelled })
        assertTrue((progress!!.items.first().result as ItemResult.Success).output.file.exists())
        assertEquals(1,root.listFiles()!!.count { it.name.startsWith("result-") })
    }
    fun batchLimitsAndConcurrency()=runBlocking {
        val orchestrator=BatchOrchestrator(engine);val input=source("noise.jpg")
        try { orchestrator.process(List(21) { BatchInput("$it",input) },Preset.CUSTOM.defaults()) { };fail("Unbounded batch") }
        catch(e: ImageProblem) { assertEquals(FailureCode.BATCH_LIMIT,e.code) }
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val slow=BatchOrchestrator(object: CompressionEngine {
            override suspend fun compress(request: CompressionRequest,progress: (CompressionProgress)->Unit): CompressionResult {
                entered.complete(Unit);release.await();return CompressionResult.Failed(CompressionFailure(FailureCode.INSUFFICIENT_MEMORY))
            }
        })
        val active=async { slow.process(listOf(BatchInput("a",input)),Preset.CUSTOM.defaults()) { } }
        entered.await()
        try { slow.process(listOf(BatchInput("b",input)),Preset.CUSTOM.defaults()) { };fail("Concurrent batch accepted") }
        catch(e: ImageProblem) { assertEquals(FailureCode.BUSY,e.code) }
        release.complete(Unit);assertEquals(1,active.await().failed)
    }
    fun nativeGate()=runBlocking {
        val active=AtomicInteger();val peak=AtomicInteger();val input=source("noise.jpg")
        val decoder: (ImageSource,com.komprexo.app.diagnostics.DiagnosticTrace)->android.graphics.Bitmap = { s,t ->
            val n=active.incrementAndGet();synchronized(peak) { peak.set(maxOf(peak.get(),n)) }
            try { Thread.sleep(10);BitmapCodec.decode(s,mode=CompressionMode.QUALITY_FIRST,trace=t) } finally { active.decrementAndGet() }
        }
        val tasks=List(4) { async(Dispatchers.Default) { ImageTransformEngine(root,decoder).transform(TransformRequest(input,OutputFormat.PNG)) } }
        assertTrue(tasks.awaitAll().all { it is ProcessingResult.Success });assertEquals(1,peak.get())
    }
    fun transformCancellation()=runBlocking {
        val input=source("noise.jpg")
        val cancelled=ImageTransformEngine(root) { _,_->throw CancellationException() }
        try { cancelled.transform(TransformRequest(input,OutputFormat.PNG));fail("Cancel swallowed") } catch(_: CancellationException) { }
        assertTrue(input.file.exists());assertFalse(root.listFiles()!!.any { it.name.startsWith("transform-") })
    }
    fun multiShare()=runBlocking {
        val outputs=listOf(success("noise.jpg",OutputFormat.JPEG),success("alpha.png",OutputFormat.PNG))
        val intent=ImageStorage(context).shareOutputs(outputs)
        assertEquals(Intent.ACTION_SEND_MULTIPLE,intent.action);assertEquals("image/*",intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0);assertEquals(2,intent.clipData!!.itemCount)
        val uris=intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!;assertEquals(2,uris.distinct().size)
        uris.forEachIndexed { index,uri ->
            assertEquals("content",uri.scheme)
            context.contentResolver.openInputStream(uri)!!.use { assertArrayEquals(outputs[index].file.readBytes(),it.readBytes()) }
        }
    }
    fun folderExport()=runBlocking {
        val outputs=listOf(success("noise.jpg",OutputFormat.JPEG),success("alpha.png",OutputFormat.PNG))
        val tree=DocumentsContract.buildTreeDocumentUri(DocumentsFixtureProvider.AUTHORITY,"root-${UUID.randomUUID()}")
        val items=ImageStorage(context).saveToFolder(outputs,tree)
        assertTrue("$items",items.all { it.destination!=null && it.error==null })
        assertEquals(2,items.map { it.destination }.distinct().size)
        items.forEach { item->context.contentResolver.openInputStream(item.destination!!)!!.use { assertArrayEquals(item.output.file.readBytes(),it.readBytes()) } }
    }
    fun partialExportAndPermissions()=runBlocking {
        val outputs=List(3) { success("noise.jpg",OutputFormat.JPEG) }
        val storage=ImageStorage(context)
        val partial=storage.saveToFolder(outputs,DocumentsContract.buildTreeDocumentUri(DocumentsFixtureProvider.AUTHORITY,"partial-${UUID.randomUUID()}"))
        assertEquals(1,partial.count { it.destination!=null });assertEquals(2,partial.count { it.error!=null })
        context.contentResolver.openInputStream(partial.first().destination!!)!!.use { assertArrayEquals(outputs.first().file.readBytes(),it.readBytes()) }
        val revoked=storage.saveToFolder(outputs,DocumentsContract.buildTreeDocumentUri(DocumentsFixtureProvider.AUTHORITY,"revoked-${UUID.randomUUID()}"))
        assertTrue(revoked.all { it.error==FailureCode.FILE_ACCESS })
        assertTrue(outputs.all { it.file.exists() })
    }
    fun exportStorageAndCancellation()=runBlocking {
        val outputs=List(3) { success("noise.jpg",OutputFormat.JPEG) };var n=0
        val items=exportSequential(outputs,ExportWriter { if(n++==1) throw IOException("ENOSPC");Uri.parse("content://fixture/$n") })
        assertEquals(2,items.count { it.destination!=null });assertEquals(FailureCode.STORAGE_FULL,items[1].error)
        var accepted=listOf<ExportItem>();n=0
        try { exportSequential(outputs,ExportWriter { if(n++==1) throw CancellationException();Uri.parse("content://fixture/$n") }) { accepted=it };fail("Cancellation swallowed") }
        catch(_: CancellationException) { }
        assertEquals(1,accepted.size);assertNotNull(accepted.first().destination)
    }
    fun shareSafetyLimits()=runBlocking {
        val output=success("noise.jpg",OutputFormat.JPEG)
        val storage=ImageStorage(context)
        try { storage.shareOutputs(List(21) { output });fail("Unbounded share") }
        catch(e: ImageProblem) { assertEquals(FailureCode.BATCH_LIMIT,e.code) }
        val shared=File(context.cacheDir,"shared").apply { mkdirs() }
        val before=shared.listFiles()!!.map { it.name }.toSet()
        val filler=File(shared,"quota-fixture-${UUID.randomUUID()}")
        try {
            RandomAccessFile(filler,"rw").use { it.setLength(128L*1024*1024+1) }
            try { storage.shareOutputs(listOf(output));fail("Share capacity ignored") }
            catch(e: ImageProblem) { assertEquals(FailureCode.STORAGE_FULL,e.code) }
        } finally { filler.delete() }
        assertEquals(before,shared.listFiles()!!.map { it.name }.toSet())
        assertTrue(output.file.exists())
    }
    fun measuredBatch()=runBlocking {
        val input=source("noise.jpg");val before=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()
        val start=System.nanoTime()
        val result=BatchOrchestrator(engine).process(List(10) { BatchInput("$it",input) },Preset.CUSTOM.defaults()) { }
        val elapsed=(System.nanoTime()-start)/1_000_000
        val after=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()
        assertEquals(10,result.completed)
        val outputs=result.items.map { (it.result as ItemResult.Success).output }
        val report=File(context.filesDir,"phase2-performance.json")
        report.writeText("""{"api":${android.os.Build.VERSION.SDK_INT},"images":10,"sourceBytesEach":${input.bytes},"sourceWidth":${input.width},"sourceHeight":${input.height},"maxHeapBytes":${Runtime.getRuntime().maxMemory()},"elapsedMs":$elapsed,"heapBeforeBytes":$before,"heapAfterBytes":$after,"outputBytes":${outputs.sumOf { it.bytes }},"nativeConcurrency":1}""")
        println("PHASE2_MEASURED ${report.readText()}")
    }
}
