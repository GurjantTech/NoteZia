package com.appgurjant.stickynotes.navigation

sealed class Screen (val route : String) {
    object Splash : Screen("splash")
    object OnboardingScreen : Screen("onboarding")
    object DashboardScreen : Screen("dashboard")
    object AppLockScreen : Screen("appLock")
    object AllNotesScreen : Screen("allNotes")
    object CreateNewNoteScreen : Screen("newNote/{noteType}?noteDescription={noteDescription}") {
        fun passNoteType(noteType: String,noteDescription : String? = ""): String {
            return "newNote/$noteType?noteDescription=$noteDescription"
        }
    }
    object NoteDetailScreen : Screen("noteDetail/{noteId}?noteType={noteType}") {
        fun passNoteId(noteId: String, noteType: String): String {
            return "noteDetail/$noteId?noteType=$noteType"
        }
    }
    object QrScanScreen : Screen("QrScanScreen")
    object SettingScreen : Screen("SettingScreen")
    object GoogleSignInScreen : Screen("GoogleSignInScreen")
    object SignaturePracticeScreen : Screen("SignaturePracticeScreen")
}