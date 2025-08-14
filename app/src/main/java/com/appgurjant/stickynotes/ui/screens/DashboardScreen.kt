package com.appgurjant.stickynotes.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material3.Checkbox
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.app.domain.model.Note
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.NoteFilterType
import com.appgurjant.stickynotes.AppUtil.userTimeFormat
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.ToggleFabMenu
import com.appgurjant.stickynotes.components.filterChips
import com.appgurjant.stickynotes.navigation.Screen

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale


// Production usage
@Composable
fun DashboardScreen(navController: NavController) {

    val noteViewModel: NoteViewModel = hiltViewModel()
    noteViewModel.getAllNotes()

//    val notes = noteViewModel.getAllNotesFromDB.collectAsState().value


    val notes by noteViewModel.filteredNotes.collectAsState()
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
    DashboardUi(navController, noteList)
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
            navController.navigate(Screen.CreateNewNoteScreen.passNoteType( AppEnum.VoiceNote.name, spokenText))
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
            .background(color = colorResource(R.color.white))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Text(modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                text = stringResource(R.string.my_notes),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 30.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0171FF), // Vibrant Blue
                            Color(0xFF6EC1FF)  // Light Sky Blue
                        )
                    )
                )
            )

            // Search bar for filtering notes
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                border = BorderStroke(0.1.dp, Color.LightGray) ,// 👈 Light border
            ) {

                TextField(
                    value = searchQuery,
                    singleLine = true,
                    onValueChange = { noteViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search note here...") },
                    modifier = Modifier
                        .fillMaxWidth().background(Color.White)
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,   // no Background
                        focusedIndicatorColor = Color.Transparent, // Remove UnderLine
                        unfocusedIndicatorColor = Color.Transparent, // Remove underline When no focus
                        disabledIndicatorColor = Color.Transparent // No indicator when disabled
                    ),
                )
            }

            filterChips(selectedFilter){ it ->
                Log.e("DashboardScreen", "Selected filter: $selectedFilter")
                selectedFilter=it
            }
            if (noteList.isNotEmpty()) {
                filteredNotes = when (selectedFilter) {
                    NoteFilterType.TEXT_NOTE -> noteList.filter { it.typeOfNote == AppEnum.TextNote.name }
                    NoteFilterType.CHECKLIST -> noteList.filter { it.typeOfNote == AppEnum.CheckList.name }
                    NoteFilterType.QUICK_CAPTURE -> noteList.filter { it.typeOfNote == AppEnum.QrNote.name }
                    NoteFilterType.VOICE_NOTE -> noteList.filter { it.typeOfNote == AppEnum.VoiceNote.name }
                    else -> noteList // ALL
                }
            }
            if (filteredNotes.size < 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Your note space is empty\ncreate your first note.",
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        textAlign = TextAlign.Center,
                        color = Color.LightGray,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }else{
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
                                .clickable {
                                    val noteId = filteredNotes[index].noteId
                                    val noteType = filteredNotes[index].typeOfNote.toString()
                                    navController.navigate(Screen.NoteDetailScreen.passNoteId(noteId,noteType))
                                },
                            border = BorderStroke(0.1.dp, Color.LightGray) ,// 👈 Light border
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(modifier = Modifier.background(Color.White).fillMaxWidth().padding(10.dp)) {
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
                                    if(it.isNotEmpty()){
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
                                    val checklistItems by produceState(initialValue = emptyList<ChecklistItem>(), note.contentJson) {
                                        value = try {
                                            val gson = Gson()
                                            val type = object : TypeToken<List<ChecklistItem>>() {}.type
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
                                        checklistItems.forEachIndexed { index , item ->
                                            if(index<2){
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Checkbox(checked = item.checked,
                                                        onCheckedChange = { checked ->
                                                            // Create a new list with the updated item
                                                             checklistItems.mapIndexed { i, listItem ->
                                                                if (i == index) listItem.copy(checked = checked) else listItem
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



        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.BottomEnd
        ) {
            ToggleFabMenu { selectedItem ->
                when (selectedItem) {
                    AppEnum.VoiceNote.name -> {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...")
                        }
                        launcher.launch(intent)
                    }

                    AppEnum.Gallery.name -> {

                    }

                    AppEnum.QrNote.name -> {
                        navController.navigate(Screen.QrScanScreen.route)
                    }

                    AppEnum.CheckList.name -> {
                        navController.navigate(Screen.CreateNewNoteScreen.passNoteType(  AppEnum.CheckList.name))
                    }

                    AppEnum.TextNote.name -> {
                        navController.navigate(Screen.CreateNewNoteScreen.passNoteType( AppEnum.TextNote.name))
                    }
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

