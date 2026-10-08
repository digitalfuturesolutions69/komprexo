package com.komprexo.app.processing

import com.komprexo.app.compression.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex

/** Sequential and bounded. Callbacks transfer successful output ownership to caller.
 * Cancellation preserves delivered successes and marks only unprocessed items cancelled. */
class BatchOrchestrator(private val engine: CompressionEngine) {
    private val gate = Mutex()
    suspend fun process(inputs: List<BatchInput>, options: PresetOptions, update: (BatchProgress) -> Unit): BatchProgress {
        if (inputs.isEmpty() || inputs.size > MAX_BATCH_IMAGES) throw ImageProblem(FailureCode.BATCH_LIMIT)
        if (!gate.tryLock()) throw ImageProblem(FailureCode.BUSY)
        var items = inputs.map { BatchItem(it) }
        try {
            update(BatchProgress(items))
            for (index in items.indices) {
                currentCoroutineContext().ensureActive()
                update(BatchProgress(items,index+1))
                val input = items[index].input
                val outcome = if (input.source == null) ItemResult.Failed(input.importError ?: FailureCode.INVALID_IMAGE)
                else try {
                    when (val result = engine.compress(CompressionRequest(input.source,options.targetBytes,
                        CompressionOptions(options.format,options.mode,options.resize)))) {
                        is CompressionResult.Success -> {
                            try { currentCoroutineContext().ensureActive() }
                            catch (e: CancellationException) { result.file.parentFile?.deleteRecursively(); throw e }
                            ItemResult.Success(result.output())
                        }
                        is CompressionResult.Failed -> ItemResult.Failed(result.failure.code)
                    }
                } catch (e: CancellationException) { throw e }
                  catch (e: ImageProblem) { ItemResult.Failed(e.code) }
                  catch (_: OutOfMemoryError) { ItemResult.Failed(FailureCode.INSUFFICIENT_MEMORY) }
                  catch (_: Exception) { ItemResult.Failed(FailureCode.INTERRUPTED) }
                items = items.toMutableList().also { it[index] = BatchItem(input,outcome) }
                update(BatchProgress(items))
            }
            return BatchProgress(items)
        } catch (e: CancellationException) {
            items = items.map { if (it.result == ItemResult.Pending) it.copy(result=ItemResult.Cancelled) else it }
            update(BatchProgress(items))
            throw e
        } finally { gate.unlock() }
    }
}
