package com.appgurjant.stickynotes.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.theme.notezyPalette

/**
 * Per-note cloud sync indicator.
 *
 *  - `isSync = 1` → filled `CloudDone` (success-tinted) → "Synced".
 *  - `isSync = 0` → outlined `CloudOff` (muted)        → "Not synced".
 *
 * Used wherever a note is rendered in a list (All Notes, Recent Stuff on the
 * dashboard, etc.). Kept tiny on purpose so it never competes visually with
 * the title; pass an explicit [size] only when you need a different scale.
 */
@Composable
fun NoteSyncBadge(
    isSync: Int,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    val palette = MaterialTheme.notezyPalette
    if (isSync == 1) {
        Icon(
            imageVector = Icons.Rounded.CloudDone,
            contentDescription = stringResource(R.string.note_sync_status_synced),
            tint = palette.successDot,
            modifier = modifier.size(size)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = stringResource(R.string.note_sync_status_pending),
            tint = palette.textMuted,
            modifier = modifier.size(size)
        )
    }
}
