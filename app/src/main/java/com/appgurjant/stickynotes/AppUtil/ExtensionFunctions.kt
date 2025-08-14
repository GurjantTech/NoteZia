package com.appgurjant.stickynotes.AppUtil

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.currentTime(): String {
    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    val currentDateTime = dayName+", "+SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())

    return currentDateTime
}

fun String.userTimeFormat(dbTime: String): String {
    val inputFormat = SimpleDateFormat("EEEE, dd MMM yyyy, HH:mm:ss", Locale.ENGLISH)
    val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH)

    val date = inputFormat.parse(dbTime)
    val formattedDate = outputFormat.format(date)

    //println(formattedDate) // Wednesday, 13 Aug 2025 at 05:59 PM
    return formattedDate
}