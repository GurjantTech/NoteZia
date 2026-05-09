package com.app.data.mapper

import com.app.data.entites.StandardResponseEntity
import com.app.data.local.entity.NoteEntity
import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.model.TextStyleConfig
import com.app.domain.util.NoteTimestampCompat
import org.json.JSONObject

/**
 * NoteEntity ↔ Note mappers.
 *
 * `isSync` is treated specially: every fresh write or update from the UI
 * resets the value to `0` so the next sync pass will pick it up. The DAO
 * later flips it to `1` once Firestore confirms the upload.
 */

private fun TextStyleConfig?.toJsonString(): String? {
    val c = this ?: return null
    return JSONObject().apply {
        put("isBold", c.isBold)
        put("isItalic", c.isItalic)
        put("isUnderline", c.isUnderline)
        put("fontSize", c.fontSize)
        put("fontFamily", c.fontFamily)
    }.toString()
}

private fun parseTextStyleJson(raw: String?): TextStyleConfig? {
    if (raw.isNullOrBlank()) return null
    return try {
        val o = JSONObject(raw)
        TextStyleConfig(
            isBold = o.optBoolean("isBold"),
            isItalic = o.optBoolean("isItalic"),
            isUnderline = o.optBoolean("isUnderline"),
            fontSize = o.optInt("fontSize", 16),
            fontFamily = o.optString("fontFamily", "Inter-Regular")
        )
    } catch (_: Exception) {
        null
    }
}

fun Note.toDomainForCreate(): NoteEntity {
    val now = System.currentTimeMillis()
    val created = when {
        createdAtMillis > 0L -> createdAtMillis
        else -> NoteTimestampCompat.parseToEpochMillis(timeStamp).takeIf { it > 0L } ?: now
    }
    val updated = when {
        updatedAtMillis > 0L -> updatedAtMillis
        else -> now
    }
    return NoteEntity(
        title = title,
        description = description,
        timeStamp = updated.toString(),
        contentJson = contentJson,
        noteType = noteType,
        isSync = 0,
        createdAtMillis = created,
        updatedAtMillis = updated,
        reminderAtMillis = reminderAtMillis,
        textStyleJson = textStyleConfig.toJsonString()
    )
}

fun Note.toDomainForUpdate(): NoteEntity {
    val now = System.currentTimeMillis()
    val priorCreated = when {
        createdAtMillis > 0L -> createdAtMillis
        else -> NoteTimestampCompat.parseToEpochMillis(timeStamp).takeIf { it > 0L } ?: now
    }
    return NoteEntity(
        id = this.noteId?.toIntOrNull() ?: 0,
        title = title,
        description = description,
        timeStamp = now.toString(),
        contentJson = contentJson,
        noteType = noteType,
        isSync = 0,
        createdAtMillis = priorCreated,
        updatedAtMillis = now,
        reminderAtMillis = reminderAtMillis,
        textStyleJson = textStyleConfig.toJsonString()
    )
}

fun NoteEntity.toDomain() = Note(
    noteId = this.id.toString(),
    title = this.title,
    description = this.description,
    timeStamp = normalizedEntityTimeStamp(this),
    contentJson = this.contentJson,
    noteType = this.noteType,
    isSync = this.isSync,
    createdAtMillis = this.createdAtMillis,
    updatedAtMillis = this.updatedAtMillis,
    reminderAtMillis = this.reminderAtMillis,
    textStyleConfig = parseTextStyleJson(textStyleJson) ?: TextStyleConfig()
)

private fun normalizedEntityTimeStamp(e: NoteEntity): String? {
    val raw = e.timeStamp?.trim().orEmpty()
    if (raw.isNotEmpty()) return raw
    if (e.updatedAtMillis > 0L) return e.updatedAtMillis.toString()
    if (e.createdAtMillis > 0L) return e.createdAtMillis.toString()
    val r = e.reminderAtMillis
    if (r != null && r > 0L) return r.toString()
    return e.timeStamp
}

/**
 * Map a freshly-downloaded remote [Note] into a [NoteEntity] suitable for an
 * upsert during two-way sync.
 *
 * - Preserves [Note.noteId] as the Room primary key so the remote document
 *   and the local row share an identifier (no duplicates across devices).
 * - Returns `null` when the remote row carries no parsable id — the merge
 *   logic skips those rather than inserting orphan rows.
 * - Always marks `isSync = 1` because the row already represents the
 *   authoritative remote state — re-uploading it would be wasteful.
 */
fun Note.toRemoteUpsertEntity(): NoteEntity? {
    val parsedId = noteId?.toIntOrNull() ?: return null
    val updated = when {
        updatedAtMillis > 0L -> updatedAtMillis
        else -> NoteTimestampCompat.parseToEpochMillis(timeStamp).takeIf { it > 0L }
            ?: System.currentTimeMillis()
    }
    val created = when {
        createdAtMillis > 0L -> createdAtMillis
        else -> NoteTimestampCompat.parseToEpochMillis(timeStamp).takeIf { it > 0L } ?: updated
    }
    return NoteEntity(
        id = parsedId,
        title = title,
        description = description,
        timeStamp = updated.toString(),
        contentJson = contentJson,
        noteType = noteType,
        isSync = 1,
        createdAtMillis = created,
        updatedAtMillis = updated,
        reminderAtMillis = reminderAtMillis,
        textStyleJson = textStyleConfig.toJsonString()
    )
}

fun StandardResponseEntity.toDomain() = StandardResponse(
    status = this.status,
    message = this.message
)
