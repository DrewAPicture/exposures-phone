package com.exposures.sync

import java.io.IOException
import retrofit2.HttpException

/** Outcome of draining one batch of dirty items — see [SyncDrainer.drain]. */
data class DrainResult(val succeeded: Int, val failed: Int)

/**
 * Generic upload-and-mark loop shared by every syncable entity type: try the upload, let the
 * caller persist success or failure, and keep going even if one item fails — a single flaky item
 * shouldn't block the rest of the batch.
 */
class SyncDrainer {
    suspend fun <T> drain(
        items: List<T>,
        upload: suspend (T) -> String,
        onSuccess: suspend (T, remoteId: String) -> Unit,
        onFailure: suspend (T, error: String) -> Unit,
    ): DrainResult {
        var succeeded = 0
        var failed = 0
        for (item in items) {
            try {
                val remoteId = upload(item)
                onSuccess(item, remoteId)
                succeeded++
            } catch (e: IOException) {
                onFailure(item, e.message ?: "network error")
                failed++
            } catch (e: HttpException) {
                onFailure(item, "HTTP ${e.code()}")
                failed++
            }
        }
        return DrainResult(succeeded, failed)
    }
}
