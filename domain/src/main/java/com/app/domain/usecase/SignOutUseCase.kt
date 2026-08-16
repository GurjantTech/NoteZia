package com.app.domain.usecase

import com.app.domain.repository.AuthRepository
import com.app.domain.repository.NoteRepository
import com.app.domain.repository.SyncRepository
import com.app.domain.repository.SyncScheduler

/**
 * Signs the user out of Firebase without deleting local notes.
 *
 * Pending cloud work is attempted once, then cancelled. Unowned notes created
 * during this session are stamped with the departing uid so a later account
 * cannot upload them. Room remains the source of truth — notes stay visible
 * after logout.
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val noteRepository: NoteRepository,
    private val syncScheduler: SyncScheduler
) {
    suspend operator fun invoke() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            runCatching { syncRepository.syncPendingNotes(user.userId) }
            noteRepository.claimUnownedNotesForUser(user.userId)
        }
        syncScheduler.cancelAll()
        authRepository.signOut()
    }
}
