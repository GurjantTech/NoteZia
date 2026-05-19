package com.appgurjant.stickynotes.ui.screens.allnotes

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.domain.model.Note
import com.app.domain.util.effectiveUpdatedMillis
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.formatNoteTimeForUi
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.components.NoteSyncBadge
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import com.appgurjant.stickynotes.ui.util.BannerAd
import com.appgurjant.stickynotes.ui.util.SetStatusBarColor

private enum class AllNotesFilter(
    val titleRes: Int,
    val noteType: String?
) {
    All(R.string.all, null),
    Text(R.string.simple_note, AppEnum.TextNote.name),
    Checklist(R.string.check_list, AppEnum.CheckList.name),
    Voice(R.string.voice_note, AppEnum.VoiceNote.name),
    Qr(R.string.scan_qr, AppEnum.QrNote.name)
}

@Composable
fun AllNotesScreen(navController: NavController) {
    // Share the activity-scoped NoteViewModel so all-notes and dashboard see
    // the same instance (required for live updates after sync). LocalActivity
    // is the lint-blessed accessor; we cast to ComponentActivity (Hilt's
    // ViewModelStoreOwner contract) once at the entry point.
    val viewModel: NoteViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val palette = MaterialTheme.notezyPalette
    val notes by viewModel.getAllNotesFromDB.collectAsState()
    val loaded by viewModel.notesInitiallyLoaded.collectAsState()
    val statusDarkIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f

    LaunchedEffect(Unit) {
        viewModel.getAllNotes()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        SetStatusBarColor(color = palette.screenBackground, darkIcons = statusDarkIcons)
    }

    var selectedFilter by rememberSaveable { mutableStateOf(AllNotesFilter.All) }

    val sortedNotes = remember(notes) {
        notes.sortedByDescending { it.effectiveUpdatedMillis() }
    }
    val filteredNotes = remember(sortedNotes, selectedFilter) {
        if (selectedFilter.noteType == null) {
            sortedNotes
        } else {
            sortedNotes.filter { it.noteType == selectedFilter.noteType }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBackground)
            .systemBarsPadding()
    ) {
        AllNotesTopBar(
            onBack = { navController.navigateUp() },
            title = stringResource(R.string.all_notes)
        )

        AllNotesFilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        when {
            !loaded -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.brandPrimary)
                }
            }

            sortedNotes.isEmpty() -> {
                AllNotesEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            filteredNotes.isEmpty() -> {
                AllNotesEmptyState(
                    title = stringResource(R.string.all_notes_filtered_empty_title),
                    subtitle = stringResource(R.string.all_notes_filtered_empty_subtitle),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredNotes,
                        key = { n -> n.noteId?.takeIf { it.isNotBlank() } ?: "${n.timeStamp}_${n.hashCode()}" }
                    ) { note ->
                        AllNoteListCard(
                            note = note,
                            onClick = {
                                val id = note.noteId.orEmpty()
                                if (id.isNotBlank()) {
                                    navController.navigate(
                                        Screen.NoteDetailScreen.passNoteId(
                                            id,
                                            note.noteType.orEmpty()
                                        )
                                    )
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        BannerAd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun AllNotesFilterRow(
    selectedFilter: AllNotesFilter,
    onFilterSelected: (AllNotesFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        items(AllNotesFilter.entries.toList()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = stringResource(filter.titleRes),
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun AllNotesTopBar(onBack: () -> Unit, title: String) {
    val palette = MaterialTheme.notezyPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_navigate_back),
                tint = palette.textPrimary
            )
        }
        Text(
            text = title,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun AllNoteListCard(note: Note, onClick: () -> Unit) {
    val palette = MaterialTheme.notezyPalette
    val checklistPreview = stringResource(R.string.all_notes_checklist_preview)
    val noPreview = stringResource(R.string.all_notes_no_preview)
    val preview = remember(note.noteId, note.description, note.contentJson, note.noteType, checklistPreview, noPreview) {
        noteSubtitlePreview(note, checklistPreview, noPreview)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.new_note),
                    fontFamily = FontFamily(Font(R.font.inter_bold)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                NoteSyncBadge(isSync = note.isSync)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preview,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatNoteTimeForUi(note),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 12.sp,
                color = palette.textMuted
            )
        }
    }
}

@Composable
private fun AllNotesEmptyState(
    title: String = stringResource(R.string.all_notes_empty_title),
    subtitle: String = stringResource(R.string.all_notes_empty_subtitle),
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.notezyPalette
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.NoteAdd,
            contentDescription = stringResource(R.string.all_notes_empty_title),
            tint = palette.textMuted,
            modifier = Modifier.padding(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = palette.textSecondary
        )
    }
}

private fun noteSubtitlePreview(note: Note, checklistPreview: String, emptyPreview: String): String {
    val description = note.description?.trim().orEmpty()
    if (description.isNotEmpty()) {
        return description.replace(Regex("\\s+"), " ").trim()
    }
    if (note.noteType == AppEnum.CheckList.name) {
        return checklistPreview
    }
    val json = note.contentJson?.trim().orEmpty()
    if (json.isEmpty() || json.startsWith("[")) {
        return emptyPreview
    }
    return json.replace(Regex("\\s+"), " ").trim()
}
