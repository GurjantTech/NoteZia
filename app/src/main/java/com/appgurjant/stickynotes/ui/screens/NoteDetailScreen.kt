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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import com.appgurjant.stickynotes.AppUtil.formatNoteTimeForUi
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
        val response = noteDeleteResponse ?: return@LaunchedEffect
        // The use case emits an explicit "error" status when Firestore deletion
        // fails for a signed-in user; in that case the local note is preserved
        // and the user stays on the detail screen so they can retry.
        if (response.status.equals("error", ignoreCase = true)) {
            Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
        } else {
            FirebaseEvent.logEvent(context, FirebaseEvent.noteDeletedSuccessEvent)
            Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
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
                textStyleConfig = viewModel.textStyleConfig,
                createdAtMillis = noteDetail.createdAtMillis,
                updatedAtMillis = noteDetail.updatedAtMillis,
                reminderAtMillis = noteDetail.reminderAtMillis,
                isSync = noteDetail.isSync
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
    val colorScheme = MaterialTheme.colorScheme
    LaunchedEffect(noteDetail.noteId) {
        viewModel.onTitleChange(noteDetail.title ?: "")
        viewModel.onDescriptionChange(noteDetail.description ?: "")
        viewModel.onTextStyleConfigChange(noteDetail.textStyleConfig ?: TextStyleConfig())
    }

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
    val titleBringIntoView = remember { BringIntoViewRequester() }
    val titleFocusScope = rememberCoroutineScope()
    val gson = remember { Gson() }
    var checklist by remember(noteDetail.noteId, noteDetail.contentJson) {
        mutableStateOf(
            try {
                val type = object : TypeToken<List<ChecklistItem>>() {}.type
                val parsed = gson.fromJson<List<ChecklistItem>>(noteDetail.contentJson.orEmpty(), type)
                if (parsed.isNullOrEmpty()) listOf(ChecklistItem("", false)) else parsed
            } catch (e: Exception) {
                listOf(ChecklistItem("", false))
            }
        )
    }

    NoteEditorKeyboardAwareColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                    contentDescription = "back",
                    tint = colorScheme.onSurface,
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

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                    contentDescription = "Delete",
                    tint = colorScheme.onSurface,
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
        },
        bottomBar = {
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp)
            )
        }
    ) {
        TextField(
            value = viewModel.noteTitle,
            onValueChange = { viewModel.onTitleChange(it) },
            placeholder = {
                Text(
                    "Untitled",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular))
                )
            },
            textStyle = LocalTextStyle.current.merge(
                TextStyle(
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    color = colorScheme.onSurface
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
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(titleBringIntoView)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        titleFocusScope.launch { titleBringIntoView.bringIntoView() }
                    }
                }
        )

        val timeLabel = formatNoteTimeForUi(noteDetail)
        if (timeLabel.isNotEmpty()) {
            Text(
                timeLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                color = colorScheme.onSurfaceVariant,
            )
        }

        if (noteDetail.noteType == AppEnum.CheckList.name) {
            ChecklistEditor(
                checklist = checklist,
                focusRequesters = focusRequesters,
                onChecklistChanged = { updated ->
                    checklist = updated
                    viewModel.currentContentJson = gson.toJson(updated)
                    viewModel.isContentModified = true
                }
            )
        } else {
            NoteEditorContent(
                noteDescription = viewModel.noteDescription,
                onDescriptionChanged = {
                    viewModel.onDescriptionChange(it)
                    viewModel.isContentModified = true
                },
                isBold = viewModel.textStyleConfig.isBold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
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
                textStyleConfig = viewModel.textStyleConfig,
                createdAtMillis = noteDetail.createdAtMillis,
                updatedAtMillis = noteDetail.updatedAtMillis,
                reminderAtMillis = noteDetail.reminderAtMillis,
                isSync = noteDetail.isSync
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
                    textStyleConfig = viewModel.textStyleConfig,
                    createdAtMillis = noteDetail.createdAtMillis,
                    updatedAtMillis = noteDetail.updatedAtMillis,
                    reminderAtMillis = noteDetail.reminderAtMillis,
                    isSync = noteDetail.isSync
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
        noteId = "1",
        title = "Sample Note Title",
        description = "This is a sample note description",
        timeStamp = "Monday, 01 Jan 2023, 10:00:00"
    )
    NoteDetailUi(rememberNavController(), note, hiltViewModel())
}