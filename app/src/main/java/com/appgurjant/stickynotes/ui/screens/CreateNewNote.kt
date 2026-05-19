package com.appgurjant.stickynotes.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.domain.model.Note
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.currentTime
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.firebase.FirebaseEvent
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import com.appgurjant.stickynotes.ui.util.BannerAd
import com.appgurjant.stickynotes.ui.util.ads.AdCounterKeys
import com.appgurjant.stickynotes.ui.util.ads.rememberAdsConfig
import com.appgurjant.stickynotes.ui.util.ads.rememberInterstitialAdManager
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import java.util.Locale

@Composable
fun CreateNewNote(navController: NavController, noteType: String, noteDescription: String? = "") {
    val viewModel: NoteViewModel = hiltViewModel()
    val palette = MaterialTheme.notezyPalette
    val context = LocalContext.current
    val interstitialAdManager = rememberInterstitialAdManager()
    val adsConfig = rememberAdsConfig()
    val noteState = viewModel.noteSaveState.collectAsState().value

    var currentNoteType by remember(noteType) { mutableStateOf(noteType) }
    var checklist by remember { mutableStateOf(listOf(ChecklistItem("", false))) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val canSave by remember(currentNoteType, viewModel.noteDescription, checklist) {
        derivedStateOf {
            when (currentNoteType) {
                AppEnum.CheckList.name -> checklist.any { it.text.trim().isNotEmpty() }
                else -> viewModel.noteDescription.trim().isNotEmpty()
            }
        }
    }

    LaunchedEffect(noteType) {
        if (!noteDescription.isNullOrBlank()) {
            viewModel.onDescriptionChange(noteDescription)
            viewModel.onTitleChange(noteType)
        }
    }

    LaunchedEffect(noteState) {
        noteState?.let {
            FirebaseEvent.logEvent(context, FirebaseEvent.noteCreatedSuccessEvent)
            navController.popBackStack(Screen.CreateNewNoteScreen.route, true)
            (context as? FragmentActivity)?.let { activity ->
                interstitialAdManager.showAdEveryN(
                    activity = activity,
                    counterKey = AdCounterKeys.NOTE_CREATED,
                    threshold = adsConfig.interstitialShowThreshold
                )
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            viewModel.onDescriptionChange(
                if (viewModel.noteDescription.isBlank()) spoken else "${viewModel.noteDescription}\n$spoken"
            )
        }
    }

    val titleBringIntoView = remember { BringIntoViewRequester() }
    val titleFocusScope = rememberCoroutineScope()

    NoteEditorKeyboardAwareColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBackground)
            .systemBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        topBar = {
            EditorTopBar(
                onBack = { navController.popBackStack() },
                onSave = {
                    val validationError = validateNoteInput(
                        noteType = currentNoteType,
                        noteDescription = viewModel.noteDescription,
                        checklist = checklist,
                        context = context
                    )
                    if (validationError != null) {
                        Toast.makeText(context, validationError, Toast.LENGTH_SHORT).show()
                        return@EditorTopBar
                    }
                    saveNewNote(
                        noteType = currentNoteType,
                        viewModel = viewModel,
                        checklist = checklist
                    )
                },
                canSave = canSave
            )
            Spacer(modifier = Modifier.height(14.dp))
        },
        bottomBar = {
            Spacer(modifier = Modifier.height(10.dp))
            EditorToolTray(
                onVoice = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    }
                    speechLauncher.launch(intent)
                },
                onImage = {},
                onList = {
                    if (currentNoteType == AppEnum.CheckList.name) {
                        currentNoteType = AppEnum.TextNote.name
                        if (viewModel.noteDescription.isBlank()) {
                            val checklistText = checklist
                                .map { it.text.trim() }
                                .filter { it.isNotBlank() }
                                .joinToString(separator = "\n")
                            if (checklistText.isNotBlank()) {
                                viewModel.onDescriptionChange(checklistText)
                            }
                        }
                    } else {
                        currentNoteType = AppEnum.CheckList.name
                        if (checklist.size == 1 && checklist.first().text.isBlank() && viewModel.noteDescription.isNotBlank()) {
                            checklist = listOf(
                                ChecklistItem(viewModel.noteDescription.trim(), false),
                                ChecklistItem("", false)
                            )
                            viewModel.onDescriptionChange("")
                        }
                    }
                },
                listLabel = if (currentNoteType == AppEnum.CheckList.name) "BLANK NOTE" else "CHECKLIST",
                listIconRes = if (currentNoteType == AppEnum.CheckList.name) R.drawable.ic_blank_note else R.drawable.ic_checklist,
                onSketch = {}
            )
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
            onValueChange = viewModel::onTitleChange,
            placeholder = {
                Text(
                    text = "Untitled Vibe...",
                    color = palette.textMuted,
                    fontFamily = FontFamily(Font(R.font.inter_bold)),
                    fontSize = 16.sp
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = palette.textPrimary,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 20.sp
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text(
                text = "TODAY, ${String().currentTime()}",
                color = palette.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (currentNoteType == AppEnum.CheckList.name) {
            ChecklistEditor(
                checklist = checklist,
                focusRequesters = focusRequesters,
                onChecklistChanged = { checklist = it }
            )
        } else {
            NoteEditorContent(
                noteDescription = viewModel.noteDescription,
                onDescriptionChanged = viewModel::onDescriptionChange,
                isBold = viewModel.textStyleConfig.isBold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditorTopBar(onBack: () -> Unit, onSave: () -> Unit, canSave: Boolean) {
    val palette = MaterialTheme.notezyPalette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onBack),
            tint = palette.textPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(palette.brandPrimary, CircleShape)
            )
            Text(
                text = "EDITING NOTE",
                color = palette.textMuted,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily(Font(R.font.inter_semibold))
            )
        }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (canSave) palette.brandPrimary else palette.textMuted.copy(alpha = 0.45f)
            ),
            modifier = Modifier.clickable(enabled = canSave, onClick = onSave)
        ) {
            Text(
                text = "SAVE",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun EditorFormatRow(
    isBold: Boolean,
    isItalic: Boolean,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("≡", color = palette.textMuted, fontSize = 15.sp)
            Text(
                "T",
                color = if (isBold) palette.brandPrimary else palette.textMuted,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onBoldClick)
            )
            Text(
                "/",
                color = if (isItalic) palette.brandPrimary else palette.textMuted,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onItalicClick)
            )
            Text("⛓", color = palette.textMuted, fontSize = 14.sp)
        }
        Text("✎", color = palette.textMuted, fontSize = 15.sp)
    }
}

@Composable
private fun EditorToolTray(
    onVoice: () -> Unit,
    onImage: () -> Unit,
    onList: () -> Unit,
    listLabel: String,
    listIconRes: Int,
    onSketch: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrayAction("VOICE", R.drawable.ic_voice_recorder, palette.brandAccent, onVoice)
//            TrayAction("IMAGE", R.drawable.ic_gallery, palette.brandPrimary, onImage)
            TrayAction(listLabel, listIconRes, palette.brandPrimary, onList)
//            TrayAction("SKETCH", R.drawable.ic_drawing, Color(0xFFFF4FA5), onSketch)
        }
    }
}

@Composable
private fun TrayAction(label: String, iconRes: Int, tint: Color, onClick: () -> Unit) {
    val palette = MaterialTheme.notezyPalette
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(palette.brandPrimary, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = palette.white,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = palette.textMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily(Font(R.font.inter_semibold))
        )
    }
}

private fun saveNewNote(
    noteType: String,
    viewModel: NoteViewModel,
    checklist: List<ChecklistItem>
) {
    if (viewModel.noteTitle.isBlank()) viewModel.noteTitle = "Untitled Note"
    when (noteType) {
        AppEnum.CheckList.name -> {
            val json = Gson().toJson(checklist)
            Log.e("CheckListJSON",json)
            viewModel.saveNote(
                Note(
                    title = viewModel.noteTitle,
                    description = "",
                    timeStamp = String().currentTime(),
                    contentJson = json,
                    noteType = noteType
                )
            )
        }
        else -> {
            viewModel.saveNote(
                Note(
                    noteId = "",
                    title = viewModel.noteTitle,
                    description = viewModel.noteDescription,
                    timeStamp = String().currentTime(),
                    noteType = noteType,
                    textStyleConfig = viewModel.textStyleConfig
                )
            )
        }
    }
}

private fun validateNoteInput(
    noteType: String,
    noteDescription: String,
    checklist: List<ChecklistItem>,
    context: android.content.Context
): String? {
    return when (noteType) {
        AppEnum.CheckList.name -> {
            if (checklist.none { it.text.trim().isNotEmpty() }) {
                context.getString(R.string.validation_checklist_required)
            } else {
                null
            }
        }
        else -> {
            if (noteDescription.trim().isEmpty()) {
                context.getString(R.string.validation_description_required)
            } else {
                null
            }
        }
    }
}

data class ChecklistItem(
    var text: String,
    var checked: Boolean
)