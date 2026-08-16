package com.app.domain.usecase

import com.app.domain.repository.NoteRepository

/**
 * After Firebase authentication succeeds (or when a signed-in session is
 * restored), claim any unowned local notes for this uid, queue every owned
 * note for upload, and start the existing background sync.
 *
 * Notes created while logged out must never be lost — they upload to this
 * account only. Re-queuing owned notes is idempotent: Firestore documents are
 * overwritten by the same Room note id.
 */
class ActivateCloudSyncUseCase(
    private val noteRepository: NoteRepository,
    private val requestSyncUseCase: RequestSyncUseCase
) {
    suspend operator fun invoke(userId: String) {
        noteRepository.claimUnownedNotesForUser(userId)
        noteRepository.markOwnedNotesPending(userId)
        requestSyncUseCase()
    }
}
