package com.appgurjant.stickynotes.AppUtil

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.currentTime(): String {
    val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
    val currentDateTime = dayName+", "+SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH).format(Date())

    return currentDateTime
}

fun String.userTimeFormat(dbTime: String): String {
try {
    val inputFormat = SimpleDateFormat("EEEE, dd MMM yyyy, HH:mm:ss", Locale.ENGLISH)
    val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH)

    val date = inputFormat.parse(dbTime)
    val formattedDate = outputFormat.format(date)

    //println(formattedDate) // Wednesday, 13 Aug 2025 at 05:59 PM
    return formattedDate
}catch (e: Exception) {
    e.printStackTrace()
    return ""
}

}