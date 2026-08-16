package com.app.data.repository

import com.app.data.auth.UserSessionStorage
import com.app.data.remote.dto.toDomain
import com.app.data.remote.dto.toFirestoreNoteDtoParsed
import com.app.data.remote.dto.toFirestoreDto
import com.app.domain.model.Note
import com.app.domain.model.UserProfile
import com.app.domain.repository.FirestoreRepository
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [FirestoreRepository].
 *
 * Document layout matches the spec exactly:
 *
 *   users/{userId}
 *     name, email, photoUrl, createdAt, isPremium, lastSyncTime
 *     └── notes/{noteId}
 *           title, content, createdAt, updatedAt, isDeleted, color, pinned
 *
 * `noteId` always equals the local Room primary key, so re-uploads overwrite
 * the same Firestore document and never create duplicates.
 */
class FirestoreRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val sessionStorage: UserSessionStorage
) : FirestoreRepository {

    override suspend fun saveUserProfile(profile: UserProfile) {
        Log.d(TAG, "Saving user profile users/${profile.userId}")
        firestore.collection(USERS)
            .document(profile.userId)
            .set(profile.toFirestoreDto(), SetOptions.merge())
            .await()
    }

    override suspend fun uploadNote(userId: String, note: Note) {
        val documentId = note.noteId?.takeIf { it.isNotBlank() }
            ?: error("Note is missing local id; cannot upload to Firestore")
        Log.d(TAG, "Uploading note users/$userId/notes/$documentId")
        firestore.collection(USERS)
            .document(userId)
            .collection(NOTES)
            .document(documentId)
            .set(note.toFirestoreDto(), SetOptions.merge())
            .await()
        Log.d(TAG, "Uploaded note users/$userId/notes/$documentId")
    }

    override suspend fun fetchAllNotes(userId: String): List<Note> {
        val snapshot = firestore.collection(USERS)
            .document(userId)
            .collection(NOTES)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            val dto = doc.toFirestoreNoteDtoParsed()
            // Honour soft deletes — never resurrect a tombstoned remote row.
            if (dto.isDeleted) return@mapNotNull null
            dto.toDomain(doc.id)
        }
    }

    override suspend fun deleteNote(userId: String, noteId: String) {
        firestore.collection(USERS)
            .document(userId)
            .collection(NOTES)
            .document(noteId)
            .delete()
            .await()
    }

    override suspend fun updateLastSyncTime(userId: String, timestamp: Long) {
        firestore.collection(USERS)
            .document(userId)
            .set(mapOf("lastSyncTime" to timestamp), SetOptions.merge())
            .await()
        sessionStorage.updateLastSyncTime(timestamp)
    }

    private companion object {
        const val USERS = "users"
        const val NOTES = "notes"
        const val TAG = "NotezyFirestore"
    }
}
