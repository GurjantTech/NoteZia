package com.app.data.remote.dto

import com.app.domain.model.Note
import com.app.domain.model.TextStyleConfig
import com.app.domain.util.NoteTimestampCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.PropertyName
import org.json.JSONObject
import java.util.Date

/**
 * Wire format for `users/{userId}/notes/{noteId}`.
 *
 * Notes are written by Firestore document ID = local Room ID, so the same
 * document is overwritten on subsequent uploads (no duplicates).
 *
 * Checklist bodies live in [contentJson] as a JSON array string; [content]
 * mirrors that payload for backward compatibility with older readers.
 *
 * `color` and `pinned` are present for forward compatibility with future UI
 * features; today we always upload defaults (0, false).
 */
data class FirestoreNoteDto(
    val title: String = "",
    val content: String = "",
    val description: String = "",
    val contentJson: String = "",
    val noteType: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val color: Int = 0,
    val pinned: Boolean = false,
    val textStyleJson: String = "",
    /** 0 means no reminder set. */
    val reminderAtMillis: Long = 0L,
    /**
     * Redundant string copy of the last update instant, stored under the
     * Firestore key `timeStamp` for clients that only read that field.
     */
    @get:PropertyName("timeStamp")
    @PropertyName("timeStamp")
    val timeStampField: String = ""
)

private fun TextStyleConfig?.toFirestoreJson(): String {
    val c = this ?: return ""
    return JSONObject().apply {
        put("isBold", c.isBold)
        put("isItalic", c.isItalic)
        put("isUnderline", c.isUnderline)
        put("fontSize", c.fontSize)
        put("fontFamily", c.fontFamily)
    }.toString()
}

private fun parseTextStyleFromFirestore(raw: String): TextStyleConfig? {
    if (raw.isBlank()) return null
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

/**
 * Map a domain [Note] into the Firestore-shaped DTO.
 */
fun Note.toFirestoreDto(now: Long = System.currentTimeMillis()): FirestoreNoteDto {
    val updated = when {
        updatedAtMillis > 0L -> updatedAtMillis
        else -> {
            val parsed = NoteTimestampCompat.parseToEpochMillis(timeStamp)
            if (parsed > 0L) parsed else now
        }
    }
    val created = when {
        createdAtMillis > 0L -> createdAtMillis
        else -> updated
    }
    val jsonBody = contentJson.orEmpty()
    val flatContent = when {
        jsonBody.isNotBlank() -> jsonBody
        else -> description?.takeIf { it.isNotBlank() }.orEmpty()
    }
    return FirestoreNoteDto(
        title = title.orEmpty(),
        content = flatContent,
        description = description.orEmpty(),
        contentJson = jsonBody,
        noteType = noteType.orEmpty(),
        createdAt = created,
        updatedAt = updated,
        isDeleted = false,
        color = 0,
        pinned = false,
        textStyleJson = textStyleConfig.toFirestoreJson(),
        reminderAtMillis = reminderAtMillis ?: 0L,
        timeStampField = updated.toString()
    )
}

/**
 * Map a Firestore document into a domain [Note].
 *
 * - The Firestore document ID becomes [Note.noteId].
 * - Checklist JSON may live in [contentJson] or only in legacy [content]
 *   (older uploads); we restore whichever carries the array payload.
 * - [noteType] falls back to `"CheckList"` when the body is a JSON array.
 */
fun FirestoreNoteDto.toDomain(noteId: String): Note {
    val trimmedJson = contentJson.trim()
    val trimmedContent = content.trim()
    val resolvedContentJson = when {
        trimmedJson.isNotEmpty() -> trimmedJson
        trimmedContent.startsWith("[") -> trimmedContent
        else -> ""
    }
    val resolvedDescription = description.takeIf { it.isNotBlank() }
        ?: if (resolvedContentJson.isBlank() && trimmedContent.isNotBlank() && !trimmedContent.startsWith("[")) {
            trimmedContent
        } else {
            ""
        }
    val inferredNoteType = when {
        noteType.isNotBlank() -> noteType
        resolvedContentJson.trimStart().startsWith("[") -> "CheckList"
        else -> ""
    }
    val resolvedStyle = parseTextStyleFromFirestore(textStyleJson) ?: TextStyleConfig()

    val updatedAtFinal = when {
        updatedAt > 0L -> updatedAt
        createdAt > 0L -> createdAt
        else -> NoteTimestampCompat.parseToEpochMillis(timeStampField)
    }
    val createdFinal = when {
        createdAt > 0L -> createdAt
        updatedAt > 0L -> updatedAt
        else -> NoteTimestampCompat.parseToEpochMillis(timeStampField).takeIf { it > 0L }
            ?: updatedAtFinal
    }

    val stampForRoom = when {
        updatedAtFinal > 0L -> updatedAtFinal.toString()
        createdFinal > 0L -> createdFinal.toString()
        timeStampField.isNotBlank() -> timeStampField
        else -> ""
    }

    return Note(
        noteId = noteId,
        title = title,
        description = resolvedDescription.ifBlank { null },
        timeStamp = stampForRoom,
        contentJson = resolvedContentJson.ifBlank { null },
        noteType = inferredNoteType,
        textStyleConfig = resolvedStyle,
        isSync = 1,
        createdAtMillis = createdFinal,
        updatedAtMillis = updatedAtFinal,
        reminderAtMillis = reminderAtMillis.takeIf { it > 0L }
    )
}

/**
 * Reads a note document using explicit field typing so [Double] / [Timestamp] /
 * string payloads don't silently zero-out [createdAt] / [updatedAt].
 */
internal fun DocumentSnapshot.toFirestoreNoteDtoParsed(): FirestoreNoteDto {
    val data = data ?: return FirestoreNoteDto()
    return FirestoreNoteDto(
        title = data.stringOrEmpty("title"),
        content = data.stringOrEmpty("content"),
        description = data.stringOrEmpty("description"),
        contentJson = data.stringOrEmpty("contentJson"),
        noteType = data.stringOrEmpty("noteType"),
        createdAt = data.longMillisOrZero("createdAt"),
        updatedAt = data.longMillisOrZero("updatedAt"),
        isDeleted = data.boolOrFalse("isDeleted"),
        color = data.intOrZero("color"),
        pinned = data.boolOrFalse("pinned"),
        textStyleJson = data.stringOrEmpty("textStyleJson"),
        reminderAtMillis = data.longMillisOrZero("reminderAtMillis"),
        timeStampField = data.stringOrEmpty("timeStamp")
    )
}

private fun Map<String, Any?>.stringOrEmpty(key: String): String =
    (this[key] as? String).orEmpty()

private fun Map<String, Any?>.boolOrFalse(key: String): Boolean =
    (this[key] as? Boolean) ?: false

private fun Map<String, Any?>.intOrZero(key: String): Int =
    when (val v = this[key]) {
        is Int -> v
        is Long -> v.toInt()
        is Double -> v.toInt()
        is Number -> v.toInt()
        else -> 0
    }

private fun Map<String, Any?>.longMillisOrZero(key: String): Long {
    val v = this[key] ?: return 0L
    return when (v) {
        is Long -> v
        is Int -> v.toLong()
        is Double -> v.toLong()
        is Float -> v.toLong()
        is Number -> v.toLong()
        is Timestamp -> v.toDate().time
        is Date -> v.time
        is String -> NoteTimestampCompat.parseToEpochMillis(v)
        else -> 0L
    }
}
