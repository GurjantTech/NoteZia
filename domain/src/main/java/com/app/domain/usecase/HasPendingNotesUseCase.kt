package com.app.domain.usecase

import com.app.domain.repository.SyncRepository

/**
 * Returns `true` when at least one local note is awaiting upload (`isSync = 0`).
 *
 * Used by the manual "Sync with Drive" gate so the UI can short-circuit the
 * ad + network flow when there is literally nothing to send.
 */
class HasPendingNotesUseCase(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Boolean = syncRepository.hasPendingNotes()
}
