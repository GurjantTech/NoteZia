package com.appgurjant.stickynotes.ui.screens


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.app.domain.model.Note
import com.app.domain.utils.UIState
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.currentTime
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.FullTextInputField
import com.appgurjant.stickynotes.components.TextInputField
import com.appgurjant.stickynotes.navigation.Screen
import com.google.gson.Gson

import kotlin.collections.set


@Composable
fun CreateNewNote(navController: NavController, noteType: String,voiceNote:String = "") {
    val viewModel: NoteViewModel = hiltViewModel()
    val context = LocalContext.current
    val noteState = viewModel.noteSaveState.collectAsState().value
    when (noteState) {
        is UIState.Loading -> {
            CircularProgressIndicator()
        }

        is UIState.Success -> {
            navController.popBackStack(Screen.CreateNewNoteScreen.route, true)
        }

        is UIState.Error -> {
            Toast.makeText(context, noteState.message, Toast.LENGTH_SHORT).show()
        }
    }

    // UI for creating a new note
    CreateNoteUi(navController, noteType, voiceNote)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteUi(navController: NavController, noteType: String,voiceNote:String = "") {
    val viewModel: NoteViewModel = hiltViewModel()
    val context = LocalContext.current

    // Initialize checklist
    var checklist by remember { mutableStateOf(listOf(ChecklistItem("",false))) }
   // Store focus requesters
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .systemBarsPadding()
            .padding(10.dp)
    ) {
// Header
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                "back",
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        navController.popBackStack()
                    },
                alignment = Alignment.TopEnd
            )

            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                "note save",
                modifier = Modifier
                    .size(40.dp)
                    .clickable {

                        if (viewModel.noteTitle == "") {
                            viewModel.noteTitle = "Untitled Note"
                        }
                        when(noteType){
                            AppEnum.VoiceNote.name, AppEnum.TextNote.name->{
                                if (noteType == AppEnum.VoiceNote.name) {
                                    viewModel.noteTitle = "Quick Capture Note"
                                }
                                if (viewModel.noteDescription.length < 3) {
                                    Toast.makeText(
                                        context,
                                        "Description must be at least 3 characters long",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@clickable
                                }
                                val note = Note(
                                    "",
                                    viewModel.noteTitle,
                                    viewModel.noteDescription,
                                    String().currentTime(),
                                    noteType = noteType
                                )
                                viewModel.saveNote(note)
                            }

                            AppEnum.QrNote.name->{
                                if (noteType == AppEnum.QrNote.name) {
                                    viewModel.noteTitle = "QR Note"
                                }
                                val note = Note(
                                    "",
                                    viewModel.noteTitle,
                                    viewModel.noteDescription,
                                    String().currentTime(),
                                    noteType = noteType
                                )
                                viewModel.saveNote(note)
                            }

                            AppEnum.CheckList.name->{
                                val json = Gson().toJson(checklist)
                                val note = Note(title = viewModel.noteTitle, description = "", timeStamp = String().currentTime(),
                                    contentJson = json, noteType = noteType)
                                viewModel.saveNote(note)
                            }
                        }



                    },
                alignment = Alignment.TopEnd
            )
        }

        // Title
        TextInputField("Title") {
            viewModel.onTitleChange(it)
        }
        // Current Time
        Text(String().currentTime(), modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            color = Color.LightGray,
        )
        // Description
        when(noteType) {
          AppEnum.TextNote.name -> {
                FullTextInputField(stringResource(R.string.new_note_discrip)) {
                    viewModel.onDescriptionChange(it)
                }
            }
            AppEnum.VoiceNote.name->{
                viewModel.onDescriptionChange(voiceNote)
                FullTextInputField(stringResource(R.string.new_note_discrip), text = voiceNote) {
                    viewModel.onDescriptionChange(it)
                }
            }
            AppEnum.QrNote.name->{
                viewModel.onDescriptionChange(voiceNote)
                FullTextInputField(stringResource(R.string.new_note_discrip),text = voiceNote) {
                    viewModel.onDescriptionChange(it)
                }
            }
            AppEnum.CheckList.name -> {
                Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                    checklist.forEachIndexed { index, item ->
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            focusRequesters[index] = focusRequester
                            if (index == checklist.lastIndex) {
                                focusRequester.requestFocus()
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            Checkbox(
                                checked = checklist[index].checked,
                                onCheckedChange = { checked ->
                                    checklist = checklist.toMutableList().also {
                                        it[index] = it[index].copy(checked = checked)
                                    }
                                }
                            )

                            TextField(
                                value =checklist[index].text,
                                onValueChange = { text ->
                                    checklist = checklist.toMutableList().also {
                                        it[index] = it[index].copy(text = text)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Enter item...",
                                        color = Color.LightGray
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        // Only add if it's the last item and not empty
                                        if (index == checklist.lastIndex && item.text.isNotBlank()) {
                                            checklist = checklist +listOf( ChecklistItem("", false))
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.textFieldColors(
                                    containerColor = Color.Transparent,   // no Background
                                    focusedIndicatorColor = Color.Transparent, // Remove UnderLine
                                    unfocusedIndicatorColor = Color.Transparent,// Remove undeline When no focus
                                    disabledIndicatorColor = Color.Transparent //

                                ),
                            )

                            Image(
                                Icons.Default.Clear,
                                "remove",
                                modifier = Modifier.clickable {
                                    checklist = checklist.toMutableList().also {
                                        it.removeAt(index)
                                    }
                                })
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .wrapContentWidth()
                            .clickable {
                                checklist = checklist + listOf(ChecklistItem("", false))
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_add),
                            "Add Icon",
                        )

                        Text(
                            "List Item",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.inter_semibold)),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
    }
}


@Preview()
@Composable
fun PreviewCreateNewNote() {
    CreateNoteUi(rememberNavController(), AppEnum.TextNote.name)
}

data class ChecklistItem(
    var text: String,
    var checked: Boolean
)