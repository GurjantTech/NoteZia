package com.appgurjant.stickynotes.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.NoteFilterType
import com.appgurjant.stickynotes.AppUtil.userTimeFormat
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.ToggleFabMenu
import com.appgurjant.stickynotes.components.filterChips
import com.appgurjant.stickynotes.components.getCurrentAppLanguage
import com.appgurjant.stickynotes.firebase.FirebaseEvent
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.util.SetStatusBarColor

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale


// Production usage

@SuppressLint("MissingPermission")
@Composable
fun DashboardScreen(navController: NavController) {
    val noteViewModel: NoteViewModel = hiltViewModel()
    noteViewModel.getAllNotes()
    val context = LocalContext.current
    val shouldShowWelcomeNotification by noteViewModel.shouldShowWelcomeNotification.collectAsState()

    Log.e("currentAppLanguage", getCurrentAppLanguage(context))
    val notes by noteViewModel.filteredNotes.collectAsState()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        SetStatusBarColor(color = colorResource(id = R.color.primary), darkIcons = true)
    }
    val noteList = notes.map {
        NoteType(
            it.noteId ?: "",
            it.title ?: "",
            it.timeStamp ?: "",
            it.description ?: "",
            it.contentJson,
            it.noteType
        )
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                if (shouldShowWelcomeNotification) {
                    showWelcomeNotification(context, noteViewModel)
                }
            }
        }
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    DashboardUi(navController, noteList)
}


@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
private fun showWelcomeNotification(context: Context, noteViewModel: NoteViewModel) {
    // First, update the flag to prevent showing it again
    noteViewModel.setWelcomeNotificationShown(false)
    val channelId = "welcome_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Welcome Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.app_logo_without_bg)
        .setContentTitle("Welcome to NoteZia 🎉")
        .setContentText("Thanks for installing! Let’s get started.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()
    NotificationManagerCompat.from(context).notify(1002, notification)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUi(
    navController: NavController,
    noteList: List<NoteType>
) {
    val noteViewModel: NoteViewModel = hiltViewModel()
    val searchQuery by noteViewModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }

    // Add this state for tracking selected filter
    var filteredNotes by remember { mutableStateOf(noteList) }

    var selectedFilter by remember { mutableStateOf(NoteFilterType.ALL) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText = matches?.firstOrNull() ?: ""
            navController.navigate(
                Screen.CreateNewNoteScreen.passNoteType(
                    AppEnum.VoiceNote.name,
                    spokenText
                )
            )
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.primary))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.white))
        )
        {
            // Custom Toolbar
            WavyToolbar(title = stringResource(R.string.my_notes) ,navController)

            // Search bar for filtering notes
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                border = BorderStroke(0.1.dp, Color.LightGray),// 👈 Light border
            )
            {

                TextField(
                    value = searchQuery,
                    singleLine = true,
                    onValueChange = { noteViewModel.updateSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.search_note_here)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                )
            }

            filterChips(selectedFilter) { it ->
                Log.e("DashboardScreen", "Selected filter: $selectedFilter")
                selectedFilter = it
            }
            if (noteList.isNotEmpty()) {
                filteredNotes = when (selectedFilter) {
                    NoteFilterType.TEXT_NOTE -> noteList.filter { it.typeOfNote == AppEnum.TextNote.name }
                    NoteFilterType.CHECKLIST -> noteList.filter { it.typeOfNote == AppEnum.CheckList.name }
                    NoteFilterType.QUICK_CAPTURE -> noteList.filter { it.typeOfNote == AppEnum.QrNote.name }
                    NoteFilterType.VOICE_NOTE -> noteList.filter { it.typeOfNote == AppEnum.VoiceNote.name }
                    else -> noteList // ALL
                }
            } else {
                filteredNotes = noteList
            }
            if (filteredNotes.size < 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.your_note_space_is_empty_create_your_first_note),
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        textAlign = TextAlign.Center,
                        color = Color.LightGray,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredNotes) { index, note ->

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = LocalIndication.current
                                ) {
                                    val noteId = filteredNotes[index].noteId
                                    val noteType = filteredNotes[index].typeOfNote.toString()
                                    navController.navigate(
                                        Screen.NoteDetailScreen.passNoteId(
                                            noteId,
                                            noteType
                                        )
                                    )
                                },
                            border = BorderStroke(0.1.dp, Color.LightGray),// 👈 Light border
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(Color.White)
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = note.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.inter_bold)),
                                )
                                Text(
                                    text = String().userTimeFormat(note.createdAt),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                                )
                                note.description.let {
                                    if (it.isNotEmpty()) {
                                        Text(
                                            text = note.description,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily(Font(R.font.inter_regular)),
                                        )
                                    }
                                }

                                // Then in your code where you want to convert the JSON:
                                if (!note.contentJson.isNullOrEmpty()) {
                                    val checklistItems by produceState(
                                        initialValue = emptyList<ChecklistItem>(),
                                        note.contentJson
                                    ) {
                                        value = try {
                                            val gson = Gson()
                                            val type =
                                                object : TypeToken<List<ChecklistItem>>() {}.type
                                            gson.fromJson(note.contentJson, type)
                                        } catch (e: Exception) {
                                            emptyList()
                                        }
                                    }

//                                    val gson = Gson()
//                                    val type = object : TypeToken<List<ChecklistItem>>() {}.type
//                                    val checklistItems: List<ChecklistItem> = gson.fromJson(note.contentJson, type)
                                    // Now you can use checklistItems
                                    if (checklistItems.isNotEmpty()) {
                                        checklistItems.forEachIndexed { index, item ->
                                            if (index < 2) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Checkbox(
                                                        checked = item.checked,
                                                        onCheckedChange = { checked ->
                                                            // Create a new list with the updated item
                                                            checklistItems.mapIndexed { i, listItem ->
                                                                if (i == index) listItem.copy(
                                                                    checked = checked
                                                                ) else listItem
                                                            }
                                                        }
                                                    )
                                                    Text(
                                                        text = item.text,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontSize = 14.sp,
                                                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                                        color = if (item.checked) Color.Gray else LocalContentColor.current
                                                    )
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }
                    }
                }
            }

        }
        ToggleFabMenu { selectedItem ->
            when (selectedItem) {
                AppEnum.VoiceNote.name -> {

                    when (getCurrentAppLanguage(context)) {
                        "hi" -> {
                            FirebaseEvent.logEvent(context, FirebaseEvent.VoiceNoteInHindiEvent)

                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "कुछ बोलें...")
                            }
                            launcher.launch(intent)
                        }

                        else -> {
                            FirebaseEvent.logEvent(context, FirebaseEvent.VoiceNoteInEnglishEvent)
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...")
                            }
                            launcher.launch(intent)
                        }
                    }
                }

                AppEnum.Gallery.name -> {

                }

                AppEnum.QrNote.name -> {
                    FirebaseEvent.logEvent(context, FirebaseEvent.qrNoteEvent)
                    navController.navigate(Screen.QrScanScreen.route)
                }

                AppEnum.CheckList.name -> {
                    FirebaseEvent.logEvent(context, FirebaseEvent.checkListNoteEvent)
                    navController.navigate(Screen.CreateNewNoteScreen.passNoteType(AppEnum.CheckList.name))
                }

                AppEnum.TextNote.name -> {
                    FirebaseEvent.logEvent(context, FirebaseEvent.blankNoteEvent)
                    navController.navigate(Screen.CreateNewNoteScreen.passNoteType(AppEnum.TextNote.name))
                }
            }

        }
    }
}

// Preview usage
@Preview
@Composable
fun PreviewDashboard() {
    val dummyNotes = listOf(
        NoteType("1", "Title 1", "Date 1", "Description 1"),
        NoteType("1", "Title 2", "Date 2", "Description 2")
    )
    DashboardUi(rememberNavController(), dummyNotes)
}


data class NoteType(
    val noteId: String,
    val title: String,
    val createdAt: String,
    val description: String,
    val contentJson: String? = "",
    val typeOfNote: String? = "",
)

