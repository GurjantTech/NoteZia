package com.appgurjant.stickynotes.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.app.domain.repository.SyncScheduler
import java.util.concurrent.TimeUnit

/**
 * WorkManager-backed [SyncScheduler]. Lives in the app module so neither the
 * data nor domain modules need a WorkManager dependency.
 *
 * Uses [ExistingWorkPolicy.KEEP] so a single sync pass will run even if
 * multiple triggers fire in quick succession (e.g. saving 5 notes in a row).
 * The pass naturally drains all currently-pending rows.
 */
class WorkManagerSyncScheduler(
    private val workManager: WorkManager
) : SyncScheduler {

    override fun requestImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<NotesSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            NotesSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(NotesSyncWorker.WORK_NAME)
    }
}
