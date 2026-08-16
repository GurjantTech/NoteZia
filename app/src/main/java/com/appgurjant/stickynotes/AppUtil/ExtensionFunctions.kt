package com.appgurjant.stickynotes.AppUtil

import com.app.domain.model.Note
import com.app.domain.util.displayEpochMillisForUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun String.currentTime(): String {
    val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
    val currentDateTime = dayName+", "+SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH).format(Date())

    return currentDateTime
}

/**
 * Relative time for note listings (Dashboard / All Notes).
 *
 * - under 1 minute → Now
 * - under 1 hour → N minutes ago
 * - same calendar day → N hours ago (or Today)
 * - yesterday → Yesterday
 * - older → date and time
 */
fun formatNoteTimeForUi(note: Note): String {
    val ms = note.displayEpochMillisForUi()
    if (ms <= 0L) return ""
    return formatMillisRelative(ms)
}

/** Full absolute date-time for the note editor. */
fun formatNoteFullTimeForUi(note: Note): String {
    val ms = note.displayEpochMillisForUi()
    if (ms <= 0L) return ""
    return formatMillisAbsolute(ms)
}

fun String.userTimeFormat(dbTime: String): String {
    val trimmed = dbTime.trim()
    if (trimmed.isEmpty()) return ""
    trimmed.toLongOrNull()?.let { return formatMillisRelative(it) }
    trimmed.toDoubleOrNull()?.let { d ->
        if (!d.isNaN() && !d.isInfinite()) return formatMillisRelative(d.toLong())
    }
    return try {
        val inputFormat = SimpleDateFormat("EEEE, dd MMM yyyy, HH:mm:ss", Locale.ENGLISH)
        val date = inputFormat.parse(trimmed) ?: return ""
        formatMillisRelative(date.time)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

private fun formatMillisAbsolute(millis: Long): String {
    return try {
        SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH)
            .format(Date(millis))
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

private fun formatMillisRelative(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - millis).coerceAtLeast(0L)

    if (diff < TimeUnit.MINUTES.toMillis(1)) {
        return "Now"
    }

    if (diff < TimeUnit.HOURS.toMillis(1)) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).coerceAtLeast(1)
        return if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
    }

    val noteCal = Calendar.getInstance().apply { timeInMillis = millis }
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }

    if (isSameCalendarDay(noteCal, nowCal)) {
        val hours = TimeUnit.MILLISECONDS.toHours(diff).coerceAtLeast(1)
        // Fresh same-day notes keep hour precision; older ones collapse to Today.
        return if (hours < 12L) {
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        } else {
            "Today"
        }
    }

    val yesterdayCal = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
    }
    if (isSameCalendarDay(noteCal, yesterdayCal)) {
        return "Yesterday"
    }

    return formatMillisAbsolute(millis).ifEmpty {
        try {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(millis))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

private fun isSameCalendarDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
