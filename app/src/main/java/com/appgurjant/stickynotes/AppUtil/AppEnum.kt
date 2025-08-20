package com.appgurjant.stickynotes.AppUtil

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.appgurjant.stickynotes.R

enum class AppEnum{
    VoiceNote,Gallery,Drawing,CheckList,TextNote,QrNote
}

enum class NoteFilterType(private val displayNameRes: (Context) -> String) {
    ALL({ it.getString(R.string.all) }),
    TEXT_NOTE({ it.getString(R.string.blank_note) }),
    CHECKLIST({ it.getString(R.string.check_list) }),
    QUICK_CAPTURE({ it.getString(R.string.quick_capture) }),
    VOICE_NOTE({ it.getString(R.string.voice_note) });

    fun getDisplayName(context: Context): String = displayNameRes(context)

}
