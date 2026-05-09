package com.app.domain.util

import com.app.domain.model.Note
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses note [Note.timeStamp] values written either as epoch millis (`"173…"`),
 * scientific/double notation from Firestore (`"1.74E12"`),
 * or the legacy UI format (`"Thursday, 09 May 2026, 14:30:00"`).
 */
object NoteTimestampCompat {
    private val legacyDisplayFormat =
        SimpleDateFormat("EEEE, dd MMM yyyy, HH:mm:ss", Locale.ENGLISH)

    fun parseToEpochMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val trimmed = raw.trim()
        trimmed.toLongOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { d ->
            if (!d.isNaN() && !d.isInfinite()) return d.toLong()
        }
        return try {
            legacyDisplayFormat.parse(trimmed)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}

/** Prefer persisted millis columns; fall back to parsing [Note.timeStamp]. */
fun Note.effectiveUpdatedMillis(): Long =
    if (updatedAtMillis > 0L) updatedAtMillis
    else NoteTimestampCompat.parseToEpochMillis(timeStamp)

fun Note.effectiveCreatedMillis(): Long =
    if (createdAtMillis > 0L) createdAtMillis
    else NoteTimestampCompat.parseToEpochMillis(timeStamp)

/**
 * Single value for sorting / list-row timestamps: last update → creation →
 * reminder → parsed legacy/millis [timeStamp].
 */
fun Note.displayEpochMillisForUi(): Long {
    if (updatedAtMillis > 0L) return updatedAtMillis
    if (createdAtMillis > 0L) return createdAtMillis
    val r = reminderAtMillis
    if (r != null && r > 0L) return r
    return NoteTimestampCompat.parseToEpochMillis(timeStamp)
}
