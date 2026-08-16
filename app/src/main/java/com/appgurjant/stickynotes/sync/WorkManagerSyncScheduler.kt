package com.appgurjant.stickynotes.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.domain.repository.SyncScheduler
import java.util.concurrent.TimeUnit

/**
 * WorkManager-backed [SyncScheduler]. Lives in the app module so neither the
 * data nor domain modules need a WorkManager dependency.
 *
 * One-shot work uses [ExistingWorkPolicy.APPEND_OR_REPLACE] so a save that
 * happens while a sync is already running still schedules a follow-up pass
 * after it. The `NetworkType.CONNECTED` constraint keeps the work queued
 * while offline and runs it automatically when connectivity returns.
 */
class WorkManagerSyncScheduler(
    private val workManager: WorkManager
) : SyncScheduler {

    override fun requestImmediateSync() {
        workManager.enqueueUniqueWork(
            NotesSyncWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            newOneTimeRequest()
        )
    }

    override fun ensurePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NotesSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NotesSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(NotesSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(NotesSyncWorker.PERIODIC_WORK_NAME)
    }

    private fun newOneTimeRequest() = OneTimeWorkRequestBuilder<NotesSyncWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
}
