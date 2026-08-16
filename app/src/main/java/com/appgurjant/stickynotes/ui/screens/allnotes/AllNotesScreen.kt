package com.appgurjant.stickynotes.ui.screens.allnotes

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.domain.model.Note
import com.app.domain.util.effectiveUpdatedMillis
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.formatNoteTimeForUi
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ads.NoteziaBannerAd
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.components.NoteSyncBadge
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import com.appgurjant.stickynotes.ui.util.SetStatusBarColor
import kotlinx.coroutines.launch

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
    val viewModel: NoteViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val palette = MaterialTheme.notezyPalette
    val notes by viewModel.getAllNotesFromDB.collectAsState()
    val loaded by viewModel.notesInitiallyLoaded.collectAsState()
    val isSignedIn by cloudSyncViewModel.currentUser.collectAsState()
    val signedIn = isSignedIn != null
    val statusDarkIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f

    LaunchedEffect(Unit) {
        viewModel.getAllNotes()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        SetStatusBarColor(color = palette.screenBackground, darkIcons = statusDarkIcons)
    }

    // Default: All selected whenever this screen is opened.
    var selectedFilters by rememberSaveable {
        mutableStateOf(setOf(AllNotesFilter.All.name))
    }
    LaunchedEffect(Unit) {
        selectedFilters = setOf(AllNotesFilter.All.name)
    }

    val selectedFilterEnums = remember(selectedFilters) {
        selectedFilters.mapNotNull { name ->
            AllNotesFilter.entries.find { it.name == name }
        }.toSet()
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val sortedNotes = remember(notes) {
        notes.sortedByDescending { it.effectiveUpdatedMillis() }
    }
    val filteredNotes = remember(sortedNotes, selectedFilterEnums) {
        filterNotes(sortedNotes, selectedFilterEnums)
    }

    // Open drawer from the right by flipping layout direction around the drawer.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AllNotesFilterDrawer(
                        selectedFilters = selectedFilterEnums,
                        onToggleFilter = { filter ->
                            selectedFilters = toggleFilter(selectedFilters, filter)
                        },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AllNotesContent(
                    navController = navController,
                    loaded = loaded,
                    sortedNotes = sortedNotes,
                    filteredNotes = filteredNotes,
                    signedIn = signedIn,
                    onOpenFilter = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

@Composable
private fun AllNotesContent(
    navController: NavController,
    loaded: Boolean,
    sortedNotes: List<Note>,
    filteredNotes: List<Note>,
    signedIn: Boolean,
    onOpenFilter: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBackground)
            .systemBarsPadding()
    ) {
        AllNotesTopBar(
            onBack = { navController.navigateUp() },
            title = stringResource(R.string.all_notes),
            onFilterClick = onOpenFilter
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
                        key = { n ->
                            n.noteId?.takeIf { it.isNotBlank() }
                                ?: "${n.timeStamp}_${n.hashCode()}"
                        }
                    ) { note ->
                        AllNoteListCard(
                            note = note,
                            isSignedIn = signedIn,
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

        NoteziaBannerAd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun AllNotesFilterDrawer(
    selectedFilters: Set<AllNotesFilter>,
    onToggleFilter: (AllNotesFilter) -> Unit,
    onClose: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        drawerContainerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.filter_notes),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.filter_notes_subtitle),
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 13.sp,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.outline)
            Spacer(modifier = Modifier.height(8.dp))

            AllNotesFilter.entries.forEach { filter ->
                val checked = selectedFilters.contains(filter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFilter(filter) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(filter.titleRes),
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = 15.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleFilter(filter) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = palette.brandPrimary,
                            uncheckedColor = palette.textMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.done),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 15.sp,
                color = palette.brandPrimary,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onClose)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun AllNotesTopBar(
    onBack: () -> Unit,
    title: String,
    onFilterClick: () -> Unit
) {
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
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(R.string.cd_filter_notes),
                tint = palette.textPrimary
            )
        }
    }
}

@Composable
private fun AllNoteListCard(note: Note, isSignedIn: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.notezyPalette
    val checklistPreview = stringResource(R.string.all_notes_checklist_preview)
    val noPreview = stringResource(R.string.all_notes_no_preview)
    val preview = remember(
        note.noteId,
        note.description,
        note.contentJson,
        note.noteType,
        checklistPreview,
        noPreview
    ) {
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
                if (isSignedIn) {
                    NoteSyncBadge(isSync = note.isSync)
                }
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

private fun filterNotes(
    notes: List<Note>,
    selected: Set<AllNotesFilter>
): List<Note> {
    if (selected.isEmpty() || selected.contains(AllNotesFilter.All)) {
        return notes
    }
    val types = selected.mapNotNull { it.noteType }.toSet()
    return notes.filter { it.noteType in types }
}

private fun toggleFilter(
    currentNames: Set<String>,
    filter: AllNotesFilter
): Set<String> {
    val current = currentNames.mapNotNull { name ->
        AllNotesFilter.entries.find { it.name == name }
    }.toMutableSet()

    when (filter) {
        AllNotesFilter.All -> {
            // Selecting All clears every other type filter.
            return setOf(AllNotesFilter.All.name)
        }
        else -> {
            if (current.contains(filter)) {
                current.remove(filter)
            } else {
                current.remove(AllNotesFilter.All)
                current.add(filter)
            }
            if (current.isEmpty()) {
                return setOf(AllNotesFilter.All.name)
            }
            return current.map { it.name }.toSet()
        }
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
