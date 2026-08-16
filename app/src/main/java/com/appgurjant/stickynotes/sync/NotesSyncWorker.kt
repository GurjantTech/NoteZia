package com.appgurjant.stickynotes.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.domain.repository.AuthRepository
import com.app.domain.usecase.HasPendingNotesUseCase
import com.app.domain.usecase.SyncPendingNotesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Background worker that drains the local "pending sync" queue.
 *
 * Scheduled by [WorkManagerSyncScheduler] with a `NetworkType.CONNECTED`
 * constraint so it runs only when the device is online. The use case itself
 * is a no-op when the user is signed out, keeping the worker safe to enqueue
 * blindly. The UI is never blocked — WorkManager runs this off the main thread.
 */
@HiltWorker
class NotesSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncPendingNotesUseCase: SyncPendingNotesUseCase,
    private val hasPendingNotesUseCase: HasPendingNotesUseCase,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Firebase Auth can restore a session a moment after process start.
        // Wait briefly so a just-saved note is not skipped as "signed out".
        val user = authRepository.getCurrentUser()
            ?: withTimeoutOrNull(AUTH_WAIT_MS) {
                authRepository.observeCurrentUser().first { it != null }
            }
        if (user == null) {
            Log.d(TAG, "No signed-in user after wait; skipping sync pass")
            return Result.success()
        }

        return runCatching {
            val result = syncPendingNotesUseCase()
            Log.d(
                TAG,
                "Sync pass: attempted=${result.attempted}, succeeded=${result.succeeded}, " +
                    "failed=${result.failed}, error=${result.errorMessage}"
            )
            when {
                result.failed > 0 || result.errorMessage != null -> Result.retry()
                hasPendingNotesUseCase() -> Result.retry()
                else -> Result.success()
            }
        }.getOrElse { error ->
            Log.w(TAG, "Sync worker failed", error)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "notes_sync_work"
        const val PERIODIC_WORK_NAME = "notes_sync_periodic"
        private const val AUTH_WAIT_MS = 5_000L
        private const val TAG = "NotesSyncWorker"
    }
}
