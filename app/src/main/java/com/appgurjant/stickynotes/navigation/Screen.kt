package com.appgurjant.stickynotes.navigation

sealed class Screen (val route : String) {
    object Splash : Screen("splash")
    object OnboardingScreen : Screen("onboarding")
    object DashboardScreen : Screen("dashboard")
    object CreateNewNoteScreen : Screen("newNote/{noteType}?voiceNote={voiceNote}") {
        fun passNoteType(noteType: String,voiceNote : String? = ""): String {
            return "newNote/$noteType?voiceNote=$voiceNote"
        }
    }
    object NoteDetailScreen : Screen("noteDetail/{noteId}?noteType={noteType}") {
        fun passNoteId(noteId: String, noteType: String): String {
            return "noteDetail/$noteId?noteType=$noteType"
        }
    }
    object QrScanScreen : Screen("QrScanScreen")

}