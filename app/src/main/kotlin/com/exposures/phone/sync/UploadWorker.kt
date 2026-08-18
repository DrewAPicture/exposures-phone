package com.exposures.phone.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.exposures.phone.ExposuresApplication

/**
 * Thin WorkManager glue — the actual drain logic lives in [UploadCoordinator], which is unit
 * tested directly; this class just bridges it into a Worker, which can't be meaningfully unit
 * tested outside WorkManager's own test harness.
 */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ExposuresApplication).container
        val coordinator = UploadCoordinator(container.repository, container.syncApi, applicationContext)
        val result = coordinator.drainAll()
        return if (result.failed == 0) Result.success() else Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "upload-sync"
        const val UNIQUE_PERIODIC_WORK_NAME = "upload-sync-periodic"
    }
}
