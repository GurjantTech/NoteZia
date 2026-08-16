package com.app.domain.repository

/**
 * Platform-agnostic hook for scheduling background sync work. The concrete
 * implementation lives in the app module (WorkManager), keeping the data and
 * domain modules free of any Android Worker dependency.
 */
interface SyncScheduler {

    /** Enqueue a one-shot sync that will run as soon as the network is available. */
    fun requestImmediateSync()

    /**
     * Ensure a periodic, network-constrained sync is scheduled so pending
     * notes retry automatically when connectivity returns.
     */
    fun ensurePeriodicSync()

    /** Cancel any pending or running sync work. Used during sign-out. */
    fun cancelAll()
}
