package com.app.domain.repository

/**
 * Platform-agnostic hook for scheduling background sync work. The concrete
 * implementation lives in the app module (WorkManager), keeping the data and
 * domain modules free of any Android Worker dependency.
 */
interface SyncScheduler {

    /** Enqueue a one-shot sync that will run as soon as the network is available. */
    fun requestImmediateSync()

    /** Cancel any pending or running sync work. Used during sign-out. */
    fun cancelAll()
}
