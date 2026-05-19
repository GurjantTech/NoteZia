package com.app.domain.model

/**
 * Result of a single sync pass. The UI layer turns this into a toast/snackbar.
 * `failures` may be non-zero even when `succeeded` > 0 — partial success is
 * normal for batched uploads.
 */
data class SyncResult(
    val attempted: Int,
    val succeeded: Int,
    val failed: Int,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = errorMessage == null && failed == 0
}
