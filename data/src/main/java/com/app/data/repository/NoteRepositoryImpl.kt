package com.app.data.repository

import com.app.data.entites.StandardResponseEntity
import com.app.data.local.dao.NoteDao
import com.app.data.local.entity.NoteEntity
import com.app.data.mapper.toDomain
import com.app.data.mapper.toDomainForCreate
import com.app.data.mapper.toDomainForUpdate
import com.app.data.mapper.toRemoteUpsertEntity
import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val authRepository: AuthRepository
) : NoteRepository {

    override suspend fun insertNote(note: Note): Flow<String> {
        val response = noteDao.saveNote(note.toDomainForCreate())
        return flow {
            if (response > 0) {
                emit("Note inserted successfully")
            } else {
                emit("Failed to insert note")
            }
        }
    }

    override suspend fun getAllNotes(): Flow<List<Note>> {
        return combine(
            authRepository.observeCurrentUser(),
            authRepository.observeLastSignedInUserId()
        ) { user, lastSignedInUserId -> user to lastSignedInUserId }
            .flatMapLatest { (user, lastSignedInUserId) ->
                noteDao.observeAllNotes().map { entities ->
                    entities
                        .filter { isVisibleToUser(it, user?.userId, lastSignedInUserId) }
                        .map { it.toDomain() }
                }
            }
    }

    override suspend fun getNoteDetailFromLocalById(notedId: String): Flow<Note> {
        val response = noteDao.getNoteById(noteId = notedId.toInt())
        return flow {
            if (response != null) {
                emit(response.toDomain())
            } else {
                throw Exception("Note not found")
            }
        }
    }

    override suspend fun updateNoteDetailInLocal(note: Note): Flow<StandardResponse> {
        val existing = note.noteId?.toIntOrNull()?.let { noteDao.getNoteById(it) }
        // Never resurrect a tombstoned row via a late edit/back-press update.
        if (existing?.isDeleted == 1) {
            return flow {
                emit(
                    StandardResponseEntity(
                        status = "success",
                        message = "Note already deleted"
                    ).toDomain()
                )
            }
        }
        val merged = if (note.ownerUserId.isNullOrBlank() && !existing?.ownerUserId.isNullOrBlank()) {
            note.copy(ownerUserId = existing?.ownerUserId)
        } else {
            note
        }
        val response = noteDao.updateNote(merged.toDomainForUpdate())
        return flow {
            if (response > 0) {
                emit(
                    StandardResponseEntity(
                        status = "success",
                        message = "Note updated successfully"
                    ).toDomain()
                )
            } else {
                throw Exception("Note not update")
            }
        }
    }

    override suspend fun deleteNoteById(noteId: Int): Flow<StandardResponse> {
        val response = noteDao.deleteNote(noteId)
        return flow {
            if (response > 0) {
                emit(
                    StandardResponseEntity(
                        status = "success",
                        message = "Note deleted successfully"
                    ).toDomain()
                )
            } else {
                throw Exception("Note not deleted")
            }
        }
    }

    override suspend fun getPendingSyncNotes(): List<Note> {
        val currentUid = authRepository.getCurrentUser()?.userId ?: return emptyList()
        return noteDao.getPendingSyncNotes()
            .filter { isSyncableForUser(it, currentUid) }
            .map { it.toDomain() }
    }

    override suspend fun markNoteSynced(noteId: Int) {
        noteDao.markNoteSynced(noteId)
    }

    override suspend fun updateSyncStatus(noteIds: List<Int>, status: Int) {
        if (noteIds.isEmpty()) return
        noteDao.updateSyncStatus(noteIds, status)
    }

    override suspend fun tombstoneNote(noteId: Int): Boolean {
        val now = System.currentTimeMillis()
        return noteDao.tombstoneNote(noteId, now, now.toString()) > 0
    }

    override suspend fun getTombstonedNotes(): List<Note> {
        val currentUid = authRepository.getCurrentUser()?.userId ?: return emptyList()
        return noteDao.getTombstonedNotes()
            .filter { isSyncableForUser(it, currentUid) }
            .map { it.toDomain() }
    }

    override suspend fun hasPendingSyncNotes(): Boolean =
        getPendingSyncNotes().isNotEmpty() || getTombstonedNotes().isNotEmpty()

    override suspend fun getAllNotesSnapshot(): List<Note> {
        val currentUid = authRepository.getCurrentUser()?.userId ?: return emptyList()
        return noteDao.getAllNotes()
            .filter { isSyncableForUser(it, currentUid) }
            .map { it.toDomain() }
    }

    override suspend fun applyMergeResult(
        remoteUpserts: List<Note>,
        uploadedLocalIds: List<Int>
    ) {
        val ownerUid = authRepository.getCurrentUser()?.userId
        val entities = remoteUpserts.mapNotNull { note ->
            val stamped = if (ownerUid != null) note.copy(ownerUserId = ownerUid) else note
            stamped.toRemoteUpsertEntity()
        }
        noteDao.applyMergeResult(entities, uploadedLocalIds)
    }

    override suspend fun claimUnownedNotesForUser(userId: String) {
        noteDao.claimUnownedNotesForUser(userId)
    }

    override suspend fun markOwnedNotesPending(userId: String) {
        noteDao.markOwnedNotesPending(userId)
    }

    private fun isVisibleToUser(
        entity: NoteEntity,
        currentUid: String?,
        lastSignedInUserId: String?
    ): Boolean {
        val owner = entity.ownerUserId?.takeIf { it.isNotBlank() }
        return when {
            currentUid != null -> owner == null || owner == currentUid
            owner == null -> true
            else -> owner == lastSignedInUserId
        }
    }

    private fun isSyncableForUser(entity: NoteEntity, currentUid: String): Boolean {
        val owner = entity.ownerUserId?.takeIf { it.isNotBlank() }
        return owner == null || owner == currentUid
    }
}
