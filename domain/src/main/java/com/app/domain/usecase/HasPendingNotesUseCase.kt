package com.app.domain.usecase

import com.app.domain.repository.SyncRepository

/**
 * Returns `true` when at least one local note is awaiting upload or a
 * tombstone still needs to be flushed to Firestore.
 */
class HasPendingNotesUseCase(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Boolean = syncRepository.hasPendingNotes()
}
