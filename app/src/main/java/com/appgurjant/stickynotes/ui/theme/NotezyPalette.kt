package com.appgurjant.stickynotes.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

@Immutable
data class NotezyPalette(
    val screenBackground: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val brandPrimary: Color,
    val brandAccent: Color,
    val successBackground: Color,
    val successBorder: Color,
    val successText: Color,
    val successDot: Color,
    val quickActionsPrimary: Color,
    val quickActionsSecondary: Color,
    val quickActionsIconContainer: Color,
    val outline: Color,
    val darkSurface: Color,
    val white: Color
)

val LightNotezyPalette = NotezyPalette(
    screenBackground = Color(0xFFF4F6FA),
    surface = Color.White,
    textPrimary = Color(0xFF101828),
    textSecondary = Color(0xFF667085),
    textMuted = Color(0xFF98A2B3),
    brandPrimary = Color(0xFF8B5CF6),
    brandAccent = Color(0xFF7C3AED),
    successBackground = Color(0xFFE8FCEB),
    successBorder = Color(0xFFB8F5C2),
    successText = Color(0xFF16A34A),
    successDot = Color(0xFF22C55E),
    quickActionsPrimary = Color(0xFFF0E7FF),
    quickActionsSecondary = Color(0xFFEFF2F8),
    quickActionsIconContainer = Color(0xFFE4E9F4),
    outline = Color(0xFFDDE3EE),
    darkSurface = Color(0xFF111F3D),
    white = Color(0xFFFFFFFF)
)

val DarkNotezyPalette = NotezyPalette(
    screenBackground = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    textPrimary = Color(0xFFE2E8F0),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    brandPrimary = Color(0xFF8B5CF6),
    brandAccent = Color(0xFFA78BFA),
    successBackground = Color(0xFF0F2F1F),
    successBorder = Color(0xFF1F5132),
    successText = Color(0xFF86EFAC),
    successDot = Color(0xFF4ADE80),
    quickActionsPrimary = Color(0xFF31203F),
    quickActionsSecondary = Color(0xFF25243A),
    quickActionsIconContainer = Color(0xFF332B4E),
    outline = Color(0xFF334155),
    darkSurface = Color(0xFF0B1733),
    white = Color(0xFFE7EDF5)
)

val LocalNotezyPalette = staticCompositionLocalOf { LightNotezyPalette }

val MaterialTheme.notezyPalette: NotezyPalette
    @Composable get() = LocalNotezyPalette.current
