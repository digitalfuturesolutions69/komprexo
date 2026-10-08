package com.komprexo.app.storage

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.core.provider.DocumentsContractCompat
import com.komprexo.app.compression.*
import com.komprexo.app.processing.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.UUID

data class ExportItem(val output: ImageOutput, val destination: Uri? = null, val error: FailureCode? = null)
fun interface ExportWriter { suspend fun write(output: ImageOutput): Uri }

/** Each successful document is retained. A later failure/cancellation cannot roll it back. */
suspend fun exportSequential(outputs: List<ImageOutput>, writer: ExportWriter, update: (List<ExportItem>) -> Unit = {}): List<ExportItem> {
    if (outputs.isEmpty() || outputs.size > MAX_BATCH_IMAGES) throw ImageProblem(FailureCode.BATCH_LIMIT)
    val completed = mutableListOf<ExportItem>()
    for (output in outputs) {
        currentCoroutineContext().ensureActive()
        val item = try { ExportItem(output,writer.write(output)) }
            catch (e: CancellationException) { throw e }
            catch (e: ImageProblem) { ExportItem(output,error=e.code) }
            catch (_: SecurityException) { ExportItem(output,error=FailureCode.FILE_ACCESS) }
            catch (e: IOException) { ExportItem(output,error=storageFailure(e)) }
            catch (_: Exception) { ExportItem(output,error=FailureCode.FILE_ACCESS) }
        completed.add(item); update(completed.toList())
    }
    return completed
}

suspend fun ImageStorage.saveOutput(output: ImageOutput, uri: Uri) = withContext(Dispatchers.IO) {
    if (uri.scheme != "content") throw ImageProblem(FailureCode.INVALID_URI)
    checkOutput(output)
    context.contentResolver.openOutputStream(uri,"w")?.use { destination -> output.file.inputStream().use { copyChecked(it,destination) } }
        ?: throw ImageProblem(FailureCode.FILE_ACCESS)
}

suspend fun ImageStorage.saveToFolder(outputs: List<ImageOutput>, tree: Uri, update: (List<ExportItem>) -> Unit = {}): List<ExportItem> = withContext(Dispatchers.IO) {
    if (tree.scheme != "content" || !DocumentsContractCompat.isTreeUri(tree)) throw ImageProblem(FailureCode.INVALID_URI)
    val parent = DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
    exportSequential(outputs,ExportWriter { output ->
        checkOutput(output)
        val document = DocumentsContract.createDocument(context.contentResolver,parent,output.format.mime,"Komprexo-${UUID.randomUUID()}.${output.format.extension}")
            ?: throw ImageProblem(FailureCode.FILE_ACCESS)
        var keep=false
        try { saveOutput(output,document); keep=true; document }
        finally { if (!keep) withContext(NonCancellable) { try { DocumentsContract.deleteDocument(context.contentResolver,document) } catch (_: Exception) { } } }
    },update)
}

suspend fun ImageStorage.shareOutputs(outputs: List<ImageOutput>): Intent {
    var ownedDirectory: File? = null
    try { return withContext(Dispatchers.IO) {
    if (outputs.isEmpty() || outputs.size > MAX_BATCH_IMAGES) throw ImageProblem(FailureCode.BATCH_LIMIT)
    outputs.forEach(::checkOutput)
    val shared = File(context.cacheDir,"shared").apply { mkdirs() }
    val existing = shared.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    if (outputs.sumOf { it.bytes } > 128L*1024*1024-existing) throw ImageProblem(FailureCode.STORAGE_FULL)
    val directory=File(shared,"batch-${UUID.randomUUID()}").also { ownedDirectory=it }
    if (!directory.mkdirs()) throw ImageProblem(FailureCode.STORAGE_FULL)
    var keep=false
    try {
        val uris=outputs.map { output ->
            ensureActive()
            val file=File(directory,"Komprexo-${UUID.randomUUID()}.${output.format.extension}")
            file.outputStream().use { destination -> output.file.inputStream().use { copyChecked(it,destination) } }
            FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        }
        val clip=ClipData.newRawUri("Komprexo",uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        val intent=Intent(if(uris.size==1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type=outputs.map { it.format.mime }.distinct().singleOrNull() ?: "image/*"
            if(uris.size==1) putExtra(Intent.EXTRA_STREAM,uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM,ArrayList(uris))
            clipData=clip;addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ensureActive();keep=true;intent
    } finally { if(!keep) directory.deleteRecursively() }
    } } catch(e: CancellationException) { ownedDirectory?.deleteRecursively();throw e }
}
private fun checkOutput(output: ImageOutput) {
    if(output.format==OutputFormat.AUTO || output.bytes<=0 || output.file.length()!=output.bytes) throw ImageProblem(FailureCode.OUTPUT_INVALID)
}
