package com.appgurjant.stickynotes.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.AppUtil.NoteFilterType
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.screens.NoteType
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.moveTo


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    hintTxt: String,
    searchQuery: StateFlow<String>,
    onSearch: (String) -> Unit
) {


    val query by searchQuery.collectAsState()
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .height(50.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { newText ->

                onSearch(newText)
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
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun OutlinedInputTextField(hint:String){
    var inputText by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = inputText,
            onValueChange = { newText-> inputText=newText},
            label = { Text(hint, color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        )

    }
}

@Composable
fun GradientBtn(text:String,onClick:()->Unit) {
Box(modifier = Modifier.width(100.dp).height(40.dp)
    .clip(RoundedCornerShape(40.dp))
    .background(brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0171FF), // Vibrant Blue
            Color(0xFF6EC1FF)  // Light Sky Blue
        )
    ))
    .clickable { onClick() },
    contentAlignment = Alignment.Center,
   ){
    Text(text, color = colorResource(R.color.white), modifier = Modifier.padding(5.dp))
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputField(hint:String, text: String?="", onTextChanged:(String)->Unit){
    var inputTxt by remember { mutableStateOf(text.toString())}
    TextField(
        value = inputTxt,
        onValueChange = {
            inputTxt=it
            onTextChanged(it) },
        placeholder = { Text(hint, color = Color.LightGray, fontSize = 25.sp, fontFamily = FontFamily(
            Font(R.font.inter_regular)))},
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent,   // no Background
            focusedIndicatorColor = Color.Transparent , // Remove UnderLine
            unfocusedIndicatorColor = Color.Transparent ,// Remove undeline When no focus
            disabledIndicatorColor = Color.Transparent //

        ),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        textStyle = TextStyle(fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 25.sp, color = Color.Black)
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTextInputField(hint:String, text:String?="",onTextChanged:(String)->Unit){
    var inputTxt by remember { mutableStateOf(text)}
    TextField(
        value = inputTxt.toString(),
        onValueChange = {
            inputTxt=it
            onTextChanged(it)
                        },
        placeholder = { Text(hint, color = Color.LightGray)},
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent,   // no Background
            focusedIndicatorColor = Color.Transparent , // Remove UnderLine
            unfocusedIndicatorColor = Color.Transparent ,// Remove undeline When no focus
            disabledIndicatorColor = Color.Transparent //

        ),
        modifier = Modifier.fillMaxSize(),
        textStyle = TextStyle(fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 16.sp, color = Color.Black)
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
            title = { Text("Delete Note", fontFamily = FontFamily(Font(R.font.inter_semibold))) },
            text = { Text("Are you sure you want to delete this note?",fontFamily = FontFamily(Font(
                R.font.inter_regular))) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("Yes",fontFamily = FontFamily(Font(R.font.inter_regular)))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text("No",fontFamily = FontFamily(Font(R.font.inter_regular)))
                }
            }
        )
    }
}

@Composable
fun ToggleFabMenu(onClickedItem:(String)-> Unit ) {
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
                AppEnum.VoiceNote.name to R.drawable.ic_voice_recorder ,
                AppEnum.QrNote.name to  R.drawable.ic_qr_code ,
//                AppEnum.Drawing.name to R.drawable.ic_drawing ,
                AppEnum.CheckList.name to  R.drawable.ic_checklist  ,
                AppEnum.TextNote.name to R.drawable.ic_text
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
                           val templable = when(label){
                                AppEnum.VoiceNote.name-> "Voice Note"
                                AppEnum.CheckList.name-> "Check List"
                                AppEnum.QrNote.name-> "Quick Capture"
                                else-> label
                            }
                            Text(templable) },
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

fun startAudioRecording(context: Context, mediaRecordeUpdate: (MediaRecorder)-> Unit  ): MediaRecorder {
    val filePath = "${context.externalCacheDir?.absolutePath}/audio_note_${System.currentTimeMillis()}.m4a"

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
                        filterType.displayName,
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
                    .padding(horizontal = 4.dp)
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