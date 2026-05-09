package com.appgurjant.stickynotes.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.domain.usecase.SyncPendingNotesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that drains the local "pending sync" queue.
 *
 * Scheduled by [WorkManagerSyncScheduler] with a `NetworkType.CONNECTED`
 * constraint so it runs only when the device is online. The use case itself
 * is a no-op when the user is signed out, keeping the worker safe to enqueue
 * blindly.
 */
@HiltWorker
class NotesSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPendingNotesUseCase: SyncPendingNotesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val result = syncPendingNotesUseCase()
            Log.d(
                TAG,
                "Sync pass: attempted=${result.attempted}, succeeded=${result.succeeded}, " +
                    "failed=${result.failed}"
            )
            // Retry once if any uploads failed; success otherwise.
            if (result.failed == 0) Result.success() else Result.retry()
        }.getOrElse { error ->
            Log.w(TAG, "Sync worker failed", error)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "notes_sync_work"
        private const val TAG = "NotesSyncWorker"
    }
}
