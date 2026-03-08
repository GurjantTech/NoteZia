package com.appgurjant.stickynotes.ui.screens


import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.app.domain.model.Note
import com.app.domain.model.TextStyleConfig
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.currentTime
import com.appgurjant.stickynotes.AppUtil.userTimeFormat
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.DeleteNoteAlertDialog
import com.appgurjant.stickynotes.components.RichTextEditor

import com.appgurjant.stickynotes.firebase.FirebaseEvent
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.util.BannerAd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


@Composable
fun NoteDetailScreen(
    navController: NavController,
    noteId: String
) {
    val viewModel: NoteViewModel = hiltViewModel()
    LaunchedEffect(noteId) {
        viewModel.getNoteById(noteId)
    }

    val noteDetail = viewModel.getNotesDetailByIdFromLocal.collectAsState().value
    if (noteDetail != null) {
        Log.e("NoteDetailScreen", "noteType: " + noteDetail.noteType.toString())
        when (noteDetail.noteType) {
            AppEnum.CheckList.name -> {
                noteDetail.contentJson?.let {
                    viewModel.updateCurrentContentJson(it)
                }
            }
            else -> {
                noteDetail.textStyleConfig?.let {
                    viewModel.onTextStyleConfigChange(it)
                }
            }

        }
        NoteDetailUi(navController, noteDetail, viewModel)
    }

// Update Note response
    val notesUpdateResponse = viewModel.notesUpdateInLocal.collectAsState().value
// Delete Note response
    val noteDeleteResponse = viewModel.notesDeleteFromLocal.collectAsState().value
    val context = LocalContext.current

    LaunchedEffect(noteDeleteResponse) {
        noteDeleteResponse?.let {
            FirebaseEvent.logEvent(context, FirebaseEvent.noteDeletedSuccessEvent)
            Toast.makeText(context, noteDeleteResponse.message, Toast.LENGTH_SHORT).show()
            navController.popBackStack(Screen.NoteDetailScreen.route, true)
        }
    }
    LaunchedEffect(notesUpdateResponse) {
        notesUpdateResponse?.let {
            FirebaseEvent.logEvent(context, FirebaseEvent.noteUpdatedSuccessEvent)
            navController.popBackStack(Screen.NoteDetailScreen.route, true)
        }
    }



    BackHandler {
        if (noteDetail != null) {
            val updatedNote = Note(
                noteId = noteDetail.noteId,
                title = viewModel.noteTitle,
                description = viewModel.noteDescription,
                timeStamp = String().currentTime(),
                contentJson = viewModel.currentContentJson,
                noteType = noteDetail.noteType,
                textStyleConfig = viewModel.textStyleConfig
            )
            viewModel.updateNote(updatedNote)

        }
        navController.popBackStack()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailUi(navController: NavController, noteDetail: Note, viewModel: NoteViewModel) {
    val context = LocalContext.current
    viewModel.onTitleChange(noteDetail.title ?: "")
    viewModel.onDescriptionChange(noteDetail.description ?: "")
    viewModel.onTextStyleConfigChange(noteDetail.textStyleConfig?: TextStyleConfig())

    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }



    DeleteNoteAlertDialog(showDeleteDialog, {
        showDeleteDialog = false
    }, {
        viewModel.deleteNoteById(noteDetail.noteId ?: "")
        showDeleteDialog = false
        Log.e("Delete", "Clicked")
    })

    val horizontalPadding = 16.dp
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.White)
        .systemBarsPadding()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )

        {
            // Header with back button and delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                    contentDescription = "back",
                    modifier = Modifier
                        .size(30.dp)

                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current
                        ) {
                            updateNote(
                                viewModel,
                                noteDetail,
                                context,
                                viewModel.isContentModified,
                                viewModel.currentContentJson,
                                navController
                            )
                        }
                )

                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current
                        ) {
                            showDeleteDialog = true
                        }
                )
            }
            // Title field (simple TextField without formatting)
            noteDetail.title?.let { title ->
                TextField(
                    value = title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    placeholder = {
                        Text(
                            "Untitled",
                            color = Color.LightGray,
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.inter_regular))
                        )
                    },
                    textStyle = LocalTextStyle.current.merge(
                        TextStyle(
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.inter_regular))
                        )
                    ),
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

            noteDetail.timeStamp?.let {
                Log.e("Time", it)
                Text(
                    String().userTimeFormat(it),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    fontSize = 14.sp,
                    fontFamily = FontFamily(
                        Font(
                            R.font.inter_regular
                        )
                    ),
                    color = Color.DarkGray,
                )
            }

            // Show either RichTextEditor or Checklist based on note type
            // Show RichTextEditor for non-checklist notes
            noteDetail.description?.let { description ->
                Log.e("Description", description)
                if(description.isNotEmpty()){
                    RichTextEditor(
                        value = description,
                        onValueChange = {
                            viewModel.onDescriptionChange(it)
                            viewModel.isContentModified = true
                        },
                        textFormatting = noteDetail.textStyleConfig?: TextStyleConfig(),
                        onFormatChange = { newConfig ->
                            Log.e("NewConfig", newConfig.toString())
                            viewModel.onTextStyleConfigChange(newConfig)
                            viewModel.isContentModified = true
                        },
                        modifier = Modifier.weight(1f),

                    )
                }

            }
            noteDetail.contentJson?.let { json ->
                if (json.isNotEmpty()) {
                    val gson = Gson()
                    var checklist by remember {
                        mutableStateOf(
                            try {
                                val type = object : TypeToken<List<ChecklistItem>>() {}.type
                                gson.fromJson<List<ChecklistItem>>(json, type).toMutableStateList()
                            } catch (e: Exception) {
                                mutableStateListOf(ChecklistItem("", false))
                            }
                        )
                    }

                    // Update the contentJson when checklist changes
                    LaunchedEffect(checklist) {
                        viewModel.currentContentJson = gson.toJson(checklist)
                        viewModel.isContentModified = true
                    }

                    // Now you can use checklistItems
                    if (checklist.isNotEmpty()) {
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
                                        checked = item.checked,
                                        onCheckedChange = { checked ->
                                            // Create a new list with the updated item
                                            val updatedList = checklist.mapIndexed { i, listItem ->
                                                if (i == index) listItem.copy(checked = checked) else listItem
                                            }
                                            // Update the state
                                            checklist = updatedList.toMutableStateList()
                                            viewModel.isContentModified = true
                                            viewModel.currentContentJson = gson.toJson(checklist)

                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colorResource(R.color.primary),
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        TextField(
                                            value = item.text,
                                            onValueChange = { text ->
                                                val updatedList = checklist.mapIndexed { i, listItem ->
                                                    if (i == index) listItem.copy(text = text) else listItem
                                                }
                                                checklist = updatedList.toMutableStateList()
                                                viewModel.isContentModified = true
                                                viewModel.currentContentJson = gson.toJson(updatedList)
                                            },
                                            textStyle = LocalTextStyle.current.copy(
                                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (item.checked) Color.Gray else LocalContentColor.current
                                            ),
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
                                                    if (index == checklist.lastIndex && item.text.isNotBlank()) {
                                                        checklist.add(ChecklistItem("", false))
                                                    }
                                                }
                                            ),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                disabledIndicatorColor = Color.Transparent

                                            )
                                        )
                                        Image(
                                            Icons.Default.Clear,
                                            "remove",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = LocalIndication.current
                                                ) {
                                                    val updatedList = checklist.toMutableList().apply {
                                                        removeAt(index)
                                                    }
                                                    checklist = updatedList.toMutableStateList()
                                                    viewModel.isContentModified = true
                                                    viewModel.currentContentJson =
                                                        gson.toJson(updatedList)
                                                }
                                        )
                                    }

                                }

                            }


                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .wrapContentWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = LocalIndication.current
                                    ) {
                                        checklist.add(ChecklistItem("", false))
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

        BannerAd(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
    }

}

private fun updateNote(
    viewModel: NoteViewModel,
    noteDetail: Note,
    context: Context,
    isContentModified: Boolean,
    updatedContentJson: String?,
    navController: NavController
) {
    val contentJson = if (isContentModified && updatedContentJson != null) {
        updatedContentJson
    } else {
        noteDetail.contentJson
    }

    when (noteDetail.noteType) {
        AppEnum.CheckList.name -> {
            val updatedNote = Note(
                noteId = noteDetail.noteId ?: "",
                title = viewModel.noteTitle,
                description = viewModel.noteDescription,
                timeStamp = String().currentTime(),
                contentJson = contentJson,
                noteType = noteDetail.noteType,
                textStyleConfig = viewModel.textStyleConfig
            )
            viewModel.updateNote(updatedNote)
        }

        else -> {
            if (viewModel.noteTitle != noteDetail.title || viewModel.noteDescription != noteDetail.description) {
                val updatedNote = Note(
                    noteId = noteDetail.noteId ?: "",
                    title = viewModel.noteTitle,
                    description = viewModel.noteDescription,
                    timeStamp = String().currentTime(),
                    contentJson = contentJson,
                    noteType = noteDetail.noteType,
                    textStyleConfig = viewModel.textStyleConfig
                )
                viewModel.updateNote(updatedNote)
            } else {
                navController.popBackStack()
            }

        }
    }


}

@Preview
@Composable
fun PreviewNoteDetail() {
    val note = Note(
        "Sample Note Title",
        "This is a sample note description",
        "Monday, 01 Jan 2023, 10:00 AM"
    )
    NoteDetailUi(rememberNavController(), note, hiltViewModel())
}