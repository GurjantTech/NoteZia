package com.app.data.repository

import com.app.data.entites.StandardResponseEntity
import com.app.data.local.dao.NoteDao
import com.app.data.mapper.toDomain
import com.app.data.mapper.toDomainForCreate
import com.app.data.mapper.toDomainForUpdate
import com.app.data.mapper.toRemoteUpsertEntity

import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.repository.NoteRepository
import com.app.domain.utils.UIState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NoteRepositoryImpl (private val noteDao: NoteDao) : NoteRepository{
    override suspend fun insertNote(note: Note) : Flow<String> {
        val response = noteDao.saveNote(note.toDomainForCreate())

      return flow {
          if (response>0) {
              emit("Note inserted successfully")
          } else {
              emit("Failed to insert note")
          }
      }


    }

    override suspend fun getAllNotes(): Flow<List<Note>> {
       val response= noteDao.getAllNotes()
        // Convert the list of NoteEntity to List<Note>
        // and emit it as a Flow
        return flow {
            if (response.isNotEmpty()) {
                emit(response.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        }



    }

    override suspend fun getNoteDetailFromLocalById(notedId: String): Flow<Note> {
        val response=noteDao.getNoteById(noteId = notedId.toInt())
        return flow {
            if (response != null) {
                emit(response.toDomain())
            } else {
                throw Exception("Note not found")
            }
        }

    }

    override suspend fun updateNoteDetailInLocal(note: Note): Flow<StandardResponse> {

       val response=noteDao.updateNote(note.toDomainForUpdate())
        return flow {
            if (response>0) {
                emit(StandardResponseEntity(
                    status = "success",
                    message = "Note updated successfully"
                ).toDomain())
            } else {
                throw Exception("Note not update")
            }
        }

    }

    override suspend fun deleteNoteById(noteId: Int): Flow<StandardResponse> {
        val response = noteDao.deleteNote(noteId)
        return flow {
            if (response > 0) {
                emit(StandardResponseEntity(
                    status = "success",
                    message = "Note deleted successfully"
                ).toDomain())
            } else {
                throw Exception("Note not deleted")
            }
        }
    }

    override suspend fun getPendingSyncNotes(): List<Note> =
        noteDao.getPendingSyncNotes().map { it.toDomain() }

    override suspend fun markNoteSynced(noteId: Int) {
        noteDao.markNoteSynced(noteId)
    }

    override suspend fun hasPendingSyncNotes(): Boolean =
        noteDao.countPendingSyncNotes() > 0

    override suspend fun getAllNotesSnapshot(): List<Note> =
        noteDao.getAllNotes().map { it.toDomain() }

    override suspend fun applyMergeResult(
        remoteUpserts: List<Note>,
        uploadedLocalIds: List<Int>
    ) {
        val entities = remoteUpserts.mapNotNull { it.toRemoteUpsertEntity() }
        noteDao.applyMergeResult(entities, uploadedLocalIds)
    }
}