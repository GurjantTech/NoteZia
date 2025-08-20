package com.appgurjant.stickynotes.firebase

import android.content.Context
import android.util.Log

import com.appgurjant.stickynotes.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics



object FirebaseEvent {
    val VoiceNoteInHindiEvent = "voice_note_hindi"
    val VoiceNoteInEnglishEvent = "voice_note_english"
    val qrNoteEvent = "qr_note_clicked"
    val checkListNoteEvent = "check_List_note_clicked"
    val blankNoteEvent = "blank_note_clicked"
    val noteCreatedSuccessEvent = "note_created"
    val noteDeletedSuccessEvent = "note_deleted"
    val noteUpdatedSuccessEvent = "note_update"
    val changeLanguageEvent = "change_language_clicked"
    val appShareEvent = "app_share"
    val appRatingEvent = "app_rate"

    fun logEvent(content: Context,eventName: String) {
        Log.e("gurjantTrack",eventName)
        if(!BuildConfig.DEBUG) {
            FirebaseAnalytics.getInstance(content).logEvent(eventName,null)
        }



    }
}