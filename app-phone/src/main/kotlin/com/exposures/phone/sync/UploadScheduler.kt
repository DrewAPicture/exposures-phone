package com.exposures.phone.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues [UploadWorker] against WorkManager's own `NetworkType.CONNECTED` constraint — that
 * constraint is what actually delivers "drains automatically once connectivity returns": work
 * enqueued while offline simply waits until the constraint is satisfied, no polling needed here.
 */
object UploadScheduler {
    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Called right after something new becomes dirty (an exposure merge, a captured photo). */
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(networkConstraint)
            .build()
        // KEEP: if a drain is already queued or running, let it finish and pick up whatever's
        // dirty at that point rather than racing a second run against it.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UploadWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Fallback safety net in case an immediate [enqueue] call is ever missed. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<UploadWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UploadWorker.UNIQUE_PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
