package com.appgurjant.stickynotes.AppUtil

enum class AppEnum{
    VoiceNote,Gallery,Drawing,CheckList,TextNote,QrNote
}

enum class NoteFilterType(val displayName: String) {
    ALL("All"),
    TEXT_NOTE("Text Note"),
    CHECKLIST("Check List"),
    QUICK_CAPTURE("Quick Capture"),
    VOICE_NOTE("Voice Note")
}
