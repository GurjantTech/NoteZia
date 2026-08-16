package com.appgurjant.stickynotes.ui.screens.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.app.domain.util.effectiveUpdatedMillis
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.formatNoteTimeForUi
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.getCurrentAppLanguage
import com.appgurjant.stickynotes.firebase.FirebaseEvent
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.components.NoteSyncBadge
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import com.appgurjant.stickynotes.ui.util.SetStatusBarColor
import com.appgurjant.stickynotes.ui.util.ShowWelcomeNotification
import java.util.Locale

private data class DashboardTokens(
    val pageBackground: Color,
    val heading: Color,
    val muted: Color,
    val accent: Color,
    val iconButtonBorder: Color,
    val searchBg: Color,
    val stripePink: Color,
    val stripeTeal: Color,
    val stripeYellow: Color
)

private data class QuickActionStyle(
    val cardBackground: Color,
    val border: Color,
    val iconContainerBackground: Color,
    val iconTint: Color
)

@Composable
private fun dashboardTokens(): DashboardTokens {
    val palette = MaterialTheme.notezyPalette
    return DashboardTokens(
        pageBackground = palette.screenBackground,
        heading = palette.textPrimary,
        muted = palette.textSecondary,
        accent = palette.brandPrimary,
        iconButtonBorder = palette.outline,
        searchBg = palette.surface,
        stripePink = palette.brandPrimary,
        stripeTeal = palette.successDot,
        stripeYellow = palette.brandAccent
    )
}

@Composable
private fun quickActionStyles(): List<QuickActionStyle> {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(isDark) {
        if (!isDark) {
            listOf(
                QuickActionStyle(
                    cardBackground = Color(0xFFFEFCE8),
                    border = Color(0xFFFDE68A),
                    iconContainerBackground = Color(0xFFFEF9C3),
                    iconTint = Color(0xFFD97706)
                ),
                QuickActionStyle(
                    cardBackground = Color(0xFFF0FDFA),
                    border = Color(0xFF99F6E4),
                    iconContainerBackground = Color(0xFFCCFBF1),
                    iconTint = Color(0xFF0D9488)
                ),
                QuickActionStyle(
                    cardBackground = Color(0xFFEFF6FF),
                    border = Color(0xFFBFDBFE),
                    iconContainerBackground = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB)
                ),
                QuickActionStyle(
                    cardBackground = Color(0xFFFFF7ED),
                    border = Color(0xFFFED7AA),
                    iconContainerBackground = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C)
                )
            )
        } else {
            listOf(
                QuickActionStyle(
                    cardBackground = Color(0xFF2A2618),
                    border = Color(0xFF854D0E).copy(alpha = 0.55f),
                    iconContainerBackground = Color(0xFF3D3314),
                    iconTint = Color(0xFFFBBF24)
                ),
                QuickActionStyle(
                    cardBackground = Color(0xFF152A26),
                    border = Color(0xFF0F766E).copy(alpha = 0.55f),
                    iconContainerBackground = Color(0xFF134038),
                    iconTint = Color(0xFF5EEAD4)
                ),
                QuickActionStyle(
                    cardBackground = Color(0xFF1A2230),
                    border = Color(0xFF2563EB).copy(alpha = 0.45f),
                    iconContainerBackground = Color(0xFF1E2D45),
                    iconTint = Color(0xFF93C5FD)
                ),
                QuickActionStyle(
                    cardBackground = Color(0xFF2A1F18),
                    border = Color(0xFFEA580C).copy(alpha = 0.45f),
                    iconContainerBackground = Color(0xFF3D2815),
                    iconTint = Color(0xFFFDBA74)
                )
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DashboardScreen(navController: NavController) {
    val tokens = dashboardTokens()
    val activity = LocalActivity.current as ComponentActivity
    val noteViewModel: NoteViewModel = hiltViewModel(activity)
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel(activity)
    noteViewModel.getAllNotes()

    val context = LocalContext.current
    val noteZiaQuotes = context.resources.getStringArray(R.array.notezia_quotes)
    val randomQuote = remember { noteZiaQuotes.random() }
    val notes by noteViewModel.filteredNotes.collectAsState()
    val searchQuery by noteViewModel.searchQuery.collectAsState()
    val shouldShowWelcomeNotification by noteViewModel.shouldShowWelcomeNotification.collectAsState()
    val currentUser by cloudSyncViewModel.currentUser.collectAsState()
    val isSignedIn = currentUser != null
    val isSyncBannerDismissed by cloudSyncViewModel.isSyncBannerDismissed.collectAsState()
    val showSyncBanner = !isSignedIn && !isSyncBannerDismissed

    val sortedNotes = remember(notes) {
        notes.sortedByDescending { it.effectiveUpdatedMillis() }
    }

    val noteList = sortedNotes.map {
        NoteType(
            noteId = it.noteId ?: "",
            title = it.title ?: "",
            displayTime = formatNoteTimeForUi(it),
            description = it.description ?: "",
            typeOfNote = it.noteType,
            isSync = it.isSync
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && shouldShowWelcomeNotification) {
            ShowWelcomeNotification(context, noteViewModel)
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            navController.navigate(
                Screen.CreateNewNoteScreen.passNoteType(AppEnum.VoiceNote.name, spoken)
            )
        }
    }

    LaunchedEffect(Unit) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        SetStatusBarColor(color = tokens.pageBackground, darkIcons = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = tokens.pageBackground,
        floatingActionButton = {
            DashboardAddNoteFab(
                onClick = {
                    navController.navigate(Screen.CreateNewNoteScreen.passNoteType(AppEnum.TextNote.name))
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            DashboardHeader(
                onProfileClick = { navController.navigate(Screen.GoogleSignInScreen.route) },
                onSettingsClick = { navController.navigate(Screen.SettingScreen.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DashboardSearch(
                query = searchQuery,
                onQueryChange = noteViewModel::updateSearchQuery
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (showSyncBanner) {
                    item(key = "sync_banner") {
                        Spacer(modifier = Modifier.height(2.dp))
                        CloudSyncPromoBanner(
                            onSignInClick = { navController.navigate(Screen.GoogleSignInScreen.route) },
                            onDismiss = { cloudSyncViewModel.dismissSyncBanner() }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                item(key = "daily_vibe") {
                    DailyVibeCard(randomQuote)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                item(key = "quick_actions") {
                    QuickActionGrid(
                        onSignaturePractice = {
                            navController.navigate(Screen.SignaturePracticeScreen.route)
                        },
                        onChecklist = {
                            FirebaseEvent.logEvent(context, FirebaseEvent.checkListNoteEvent)
                            navController.navigate(Screen.CreateNewNoteScreen.passNoteType(AppEnum.CheckList.name))
                        },
                        onScanQr = {
                            FirebaseEvent.logEvent(context, FirebaseEvent.qrNoteEvent)
                            navController.navigate(Screen.QrScanScreen.route)
                        },
                        onVoice = {
                            when (getCurrentAppLanguage(context)) {
                                "hi" -> {
                                    FirebaseEvent.logEvent(context, FirebaseEvent.VoiceNoteInHindiEvent)
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "कुछ बोलें...")
                                    }
                                    voiceLauncher.launch(intent)
                                }
                                else -> {
                                    FirebaseEvent.logEvent(context, FirebaseEvent.VoiceNoteInEnglishEvent)
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...")
                                    }
                                    voiceLauncher.launch(intent)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item(key = "recent_header") {
                    RecentHeader(
                        onSeeAllClick = { navController.navigate(Screen.AllNotesScreen.route) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (noteList.isEmpty()) {
                    item(key = "empty_notes") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your note space is empty.\nCreate your first note.",
                                color = tokens.muted,
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.inter_regular))
                            )
                        }
                    }
                } else {
                    items(
                        items = noteList.take(5),
                        key = { it.noteId }
                    ) { note ->
                        RecentNoteCard(
                            note = note,
                            isSignedIn = isSignedIn,
                            onClick = {
                                navController.navigate(
                                    Screen.NoteDetailScreen.passNoteId(
                                        note.noteId,
                                        note.typeOfNote.orEmpty()
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val tokens = dashboardTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NoteZia",
            fontSize = 33.sp,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            color = tokens.heading
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.notezyPalette.surface)
                    .border(1.dp, tokens.iconButtonBorder, CircleShape)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = tokens.heading, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.notezyPalette.surface)
                    .border(1.dp, tokens.iconButtonBorder, CircleShape)
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = tokens.heading, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    val tokens = dashboardTokens()
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.notezyPalette.surface)
            .border(1.dp, tokens.iconButtonBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tokens.heading, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DashboardSearch(query: String, onQueryChange: (String) -> Unit) {
    val tokens = dashboardTokens()
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = tokens.muted
            )
        },
        placeholder = {
            Text(
                text = "Search your thoughts...",
                color = tokens.muted,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 14.sp
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = tokens.searchBg,
            unfocusedContainerColor = tokens.searchBg,
            disabledContainerColor = tokens.searchBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(3.dp, RoundedCornerShape(14.dp))
    )
}

@Composable
private fun DailyVibeCard(randomQuote: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8F55FF), Color(0xFFE75BB5))
                )
            )
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB596).copy(alpha = 0.22f))
        )
        Column {
            Text(
                text = "DAILY VIBE",
                color = Color(0xFFF5DE8B),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = randomQuote,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily(Font(R.font.inter_bold))
            )
        }
    }
}

@Composable
private fun QuickActionGrid(
    onSignaturePractice: () -> Unit,
    onChecklist: () -> Unit,
    onScanQr: () -> Unit,
    onVoice: () -> Unit
) {
    val styles = quickActionStyles()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.signature_practice),
                style = styles[0],
                iconRes = R.drawable.ic_drawing,
                onClick = onSignaturePractice
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.check_list),
                style = styles[1],
                iconRes = R.drawable.ic_checklist,
                onClick = onChecklist
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.scan_qr),
                style = styles[2],
                iconRes = R.drawable.ic_qr_code,
                onClick = onScanQr
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.voice_note),
                style = styles[3],
                iconRes = R.drawable.ic_voice_recorder,
                onClick = onVoice
            )
        }
    }
}

private val QuickActionCardShape = RoundedCornerShape(15.dp)
private val QuickActionIconContainerShape = RoundedCornerShape(14.dp)

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    title: String,
    style: QuickActionStyle,
    iconRes: Int,
    onClick: () -> Unit
) {
    val tokens = dashboardTokens()
    Card(
        modifier = modifier
            .height(96.dp)
            .clip(QuickActionCardShape)
            .border(1.dp, style.border, QuickActionCardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = QuickActionCardShape,
        colors = CardDefaults.cardColors(containerColor = style.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(QuickActionIconContainerShape)
                    .background(style.iconContainerBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = style.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.heading,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentHeader(onSeeAllClick: () -> Unit) {
    val tokens = dashboardTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.recent_stuff),
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 14.sp,
            color = tokens.heading
        )
        Text(
            text = stringResource(R.string.see_all),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = 12.sp,
            color = tokens.accent,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSeeAllClick
            )
        )
    }
}

@Composable
private fun RecentNoteCard(note: NoteType, isSignedIn: Boolean, onClick: () -> Unit) {
    val tokens = dashboardTokens()
    val badge = when (note.typeOfNote) {
        AppEnum.CheckList.name -> "URGENT"
        AppEnum.QrNote.name -> "CREATIVE"
        else -> "PLANNING"
    }
    val stripeColor = when (badge) {
        "URGENT" -> tokens.stripeTeal
        "CREATIVE" -> tokens.stripeYellow
        else -> tokens.stripePink
    }
    val badgeBg = stripeColor.copy(alpha = 0.2f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.notezyPalette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(stripeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifEmpty { "Untitled Note" },
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = tokens.heading,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSignedIn) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NoteSyncBadge(isSync = note.isSync, size = 12.dp)
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = note.displayTime,
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = 10.sp,
                    color = tokens.muted
                )
            }
//            Text(
//                text = "⋮",
//                color = DashboardTokens.muted,
//                fontSize = 16.sp
//            )
        }
    }
}

/** End-aligned FAB; Scaffold places it above [bottomBar] with standard bottom-end insets. */
@Composable
private fun DashboardAddNoteFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .padding(end = 4.dp, bottom = 6.dp),
        containerColor = MaterialTheme.notezyPalette.darkSurface,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.new_note)
        )
    }
}

data class NoteType(
    val noteId: String,
    val title: String,
    val displayTime: String,
    val description: String,
    val typeOfNote: String? = "",
    val isSync: Int = 0,
)

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen(rememberNavController())
    }
}

