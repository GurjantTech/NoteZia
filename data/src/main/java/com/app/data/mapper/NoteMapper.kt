package com.app.data.mapper

import com.app.data.entites.StandardResponseEntity
import com.app.data.local.entity.NoteEntity
import com.app.domain.model.Note
import com.app.domain.model.StandardResponse


fun Note.toDomainForCreate() = NoteEntity(
    title = title,
    description = description,
    timeStamp = timeStamp,
    contentJson =contentJson,
    noteType = noteType
)

fun Note.toDomainForUpdate() = NoteEntity(
    id = this.noteId?.toIntOrNull() ?: 0, // Must have correct ID here
    title = title,
    description = description,
    timeStamp = timeStamp,
    contentJson =contentJson,
    noteType = noteType
)
fun NoteEntity.toDomain()= Note(
         noteId = this.id.toString(),
        title = this.title,
        description = this.description,
        timeStamp = this.timeStamp,
        contentJson = this.contentJson,
        noteType = this.noteType)

fun StandardResponseEntity.toDomain() = StandardResponse(
    status = this.status,
    message = this.message
)



