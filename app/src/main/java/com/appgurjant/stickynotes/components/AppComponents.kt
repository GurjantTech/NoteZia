package com.appgurjant.stickynotes.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.domain.model.Note
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.NoteFilterType
import com.appgurjant.stickynotes.AppUtil.currentTime
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.screens.ChecklistItem
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import kotlin.toString


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    hintTxt: String,
    searchQuery: StateFlow<String>,
    onSearch: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery.collectAsState().value,
        onValueChange = { onSearch(it) },
        singleLine = true,
        shape = RoundedCornerShape(50),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon"
            )
        },
        label = {
            Text(
                hintTxt,
                color = Color.LightGray,
                fontFamily = FontFamily(Font(R.font.inter_regular))
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun OutlinedInputTextField(hint: String) {
    var inputText by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = inputText,
            onValueChange = { newText -> inputText = newText },
            label = { Text(hint, color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        )

    }
}

@Composable
fun GradientBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colorResource(R.color.white), modifier = Modifier.padding(5.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputField(hint: String, voiceNote: String, text: String? = "", onTextChanged: (String) -> Unit) {
    var inputTxt by remember { mutableStateOf(text.toString()) }
    Log.e("speakNote","on CreateNewNote TextInputField ${voiceNote.toString()}")
    TextField(
        value = if(!voiceNote.isNullOrEmpty()){
         "Voice Note"
        }else{
            inputTxt
        },
        onValueChange = {
            inputTxt = it
            onTextChanged(it)

        },
        placeholder = {
            Text(
                hint, color = Color.LightGray, fontSize = 25.sp, fontFamily = FontFamily(
                    Font(R.font.inter_regular)
                )
            )
        },
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
            .padding(top = 10.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 25.sp, color = Color.Black
        )
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTextInputField(hint: String, text: String? = "", onTextChanged: (String) -> Unit) {
    var inputTxt by remember { mutableStateOf(text) }
    TextField(
        value = inputTxt.toString(),
        onValueChange = {
            inputTxt = it
            onTextChanged(it)
        },
        placeholder = { Text(hint, color = Color.LightGray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        textStyle = TextStyle(
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 16.sp, color = Color.Black
        )
    )

}

@Composable
fun Context.HideKeyboard() {
    val keyboardController = LocalSoftwareKeyboardController.current
    keyboardController?.hide()
}

@Composable
fun DeleteNoteAlertDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(
                    stringResource(R.string.delete_note),
                    fontFamily = FontFamily(Font(R.font.inter_semibold))
                )
            },
            text = {
                Text(
                    stringResource(R.string.are_you_sure_you_want_to_delete_this_note),
                    fontFamily = FontFamily(
                        Font(
                            R.font.inter_regular
                        )
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.confirm), fontFamily = FontFamily(Font(R.font.inter_regular)))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.cancel), fontFamily = FontFamily(Font(R.font.inter_regular)))
                }
            }
        )
    }
}

@Composable
fun ToggleFabMenu(onClickedItem: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 45f else 0f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            // Buttons List
            val buttons = listOf(
                AppEnum.TextNote.name to R.drawable.ic_blank_note,
                AppEnum.CheckList.name to R.drawable.ic_checklist,
                AppEnum.VoiceNote.name to R.drawable.ic_voice_recorder,
                AppEnum.QrNote.name to R.drawable.ic_qr_code
//                AppEnum.Drawing.name to R.drawable.ic_drawing ,


            )

            // Show/Hide child buttons with animation
            buttons.forEach { (label, icon) ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    ExtendedFloatingActionButton(
                        text = {
                            val templable = when (label) {
                                AppEnum.VoiceNote.name -> stringResource(R.string.voice_note)
                                AppEnum.CheckList.name -> stringResource(R.string.check_list)
                                AppEnum.QrNote.name -> stringResource(R.string.quick_capture)
                                AppEnum.TextNote.name -> stringResource(R.string.blank_note)
                                else -> label
                            }
                            Text(templable)
                        },
                        icon = { Icon(painterResource(id = icon), contentDescription = label) },
                        onClick = {

                            onClickedItem(label)
                            expanded = false
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Main FAB
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = colorResource(R.color.primary),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Menu",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

fun startAudioRecording(
    context: Context,
    mediaRecordeUpdate: (MediaRecorder) -> Unit
): MediaRecorder {
    val filePath =
        "${context.externalCacheDir?.absolutePath}/audio_note_${System.currentTimeMillis()}.m4a"

    val recorder = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setOutputFile(filePath)
        prepare()
        start()
    }
    mediaRecordeUpdate(recorder)
    return recorder
}


@Composable
fun AudioPlayerScreen(audioFilePath: String) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (isPlaying) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPlaying = false
            } else {
                val file = File(audioFilePath)
                if (file.exists()) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            isPlaying = false
                        }
                    }
                    isPlaying = true
                }
            }
        }
    ) {
        Text(if (isPlaying) "Stop" else "Play")
    }
}

@Composable
fun takeCameraPermission(isPermissionGranted: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                isPermissionGranted(false)
            } else {
                isPermissionGranted(true)
            }
        }
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
}

@Composable
fun AlertDialogBox(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = stringResource(R.string.permission_required))
        },
        text = {
            Text(stringResource(R.string.camera_permission_is_needed_to_proceed_please_allow_the_permission))
        },
        confirmButton = {
            Button(onClick = {
                onConfirm()
            }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                onDismiss()
            }

            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ScanningAnimatedPreloader(modifier: Modifier = Modifier) {
    val preloaderLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.scanning_anim
        )
    )

    val preloaderProgress by animateLottieCompositionAsState(
        preloaderLottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )


    LottieAnimation(
        composition = preloaderLottieComposition,
        progress = preloaderProgress,
        modifier = modifier
    )
}

@Composable
fun filterChips(
    filterList: NoteFilterType,
    onFilterChanged: (NoteFilterType) -> Unit
) {
    // Horizontal scrollable row for chips
    val context = LocalContext.current
    SingleRowScrollableContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        NoteFilterType.values().forEach { filterType ->
            val isSelected = filterList == filterType
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChanged(filterType) },
                label = {
                    Text(
                        filterType.getDisplayName(context),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorResource(R.color.primary),
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color.LightGray,
                ),
                modifier = Modifier
                    .height(36.dp)
            )
        }
    }
}


// Helper composable for horizontal scrolling
@Composable
private fun SingleRowScrollableContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}


@Composable
fun getNotificationPermission(onPermissionGranted: (Boolean) -> Unit) {

}


fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun getCurrentAppLanguage(context: Context): String {
    val locales = context.resources.configuration.locales[0].language
    return if (!locales.isNullOrEmpty()) {
        locales ?: "en" // fallback to English
    } else {
        "en" // default
    }
}

@Composable
fun CreateNoteToolbar(navController: NavController,viewModel: NoteViewModel,noteType: String,context: Context,checklist: List<ChecklistItem>){
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_back),
            "back",
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) {
                    navController.popBackStack()
                },
            alignment = Alignment.TopEnd
        )

        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_check),
            "note save",
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) {

                    if (viewModel.noteTitle == "") {
                        viewModel.noteTitle = "Untitled Note"
                    }
                    when (noteType) {
                        AppEnum.VoiceNote.name, AppEnum.TextNote.name -> {
                            if (noteType == AppEnum.VoiceNote.name) {
                                viewModel.noteTitle = context.getString(R.string.voice_note)
                            }
                            if (viewModel.noteDescription.length < 3) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.description_must_be_at_least_3_characters_long),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@clickable
                            }
                            val note = Note(
                                "",
                                viewModel.noteTitle,
                                viewModel.noteDescription,
                                String().currentTime(),
                                noteType = noteType,
                                textStyleConfig = viewModel.textStyleConfig


                            )
                            viewModel.saveNote(note)
                        }

                        AppEnum.QrNote.name -> {
                            if (noteType == AppEnum.QrNote.name) {
                                viewModel.noteTitle = context.getString(R.string.quick_capture)
                            }
                            val note = Note(
                                "",
                                viewModel.noteTitle,
                                viewModel.noteDescription,
                                String().currentTime(),
                                noteType = noteType,
                                textStyleConfig = viewModel.textStyleConfig
                            )
                            viewModel.saveNote(note)
                        }

                        AppEnum.CheckList.name -> {
                            val json = Gson().toJson(checklist)
                            val note = Note(
                                title = viewModel.noteTitle,
                                description = "",
                                timeStamp = String().currentTime(),
                                contentJson = json,
                                noteType = noteType
                            )
                            viewModel.saveNote(note)
                        }
                    }


                },
            alignment = Alignment.TopEnd
        )
    }
}