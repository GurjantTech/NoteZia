package com.appgurjant.stickynotes.AppUtil

import com.app.domain.model.Note
import com.app.domain.util.displayEpochMillisForUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.currentTime(): String {
    val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
    val currentDateTime = dayName+", "+SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH).format(Date())

    return currentDateTime
}

/**
 * Formats a stored time for headers / lists. Prefer domain millis columns,
 * fallbacks, and robust parsing so every row shows its own instant — not only
 * the latest note.
 */
fun formatNoteTimeForUi(note: Note): String {
    val ms = note.displayEpochMillisForUi()
    if (ms <= 0L) return ""
    return "".userTimeFormat(ms.toString())
}

fun String.userTimeFormat(dbTime: String): String {
    val trimmed = dbTime.trim()
    if (trimmed.isEmpty()) return ""
    trimmed.toLongOrNull()?.let { return formatMillisForDisplay(it) }
    trimmed.toDoubleOrNull()?.let { d ->
        if (!d.isNaN() && !d.isInfinite()) return formatMillisForDisplay(d.toLong())
    }
    return try {
        val inputFormat = SimpleDateFormat("EEEE, dd MMM yyyy, HH:mm:ss", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH)

        val date = inputFormat.parse(trimmed)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

private fun formatMillisForDisplay(millis: Long): String {
    return try {
        val outputFormat =
            SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH)
        outputFormat.format(Date(millis))
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}
