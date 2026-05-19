package com.appgurjant.stickynotes.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import kotlinx.coroutines.launch

/**
 * Scrollable note body with IME insets so focused fields stay above the keyboard.
 * [topBar] stays fixed; [content] scrolls when it exceeds the remaining height.
 */
@Composable
fun NoteEditorKeyboardAwareColumn(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .imePadding()
    ) {
        topBar()
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(scrollState)
                .fillMaxWidth()
        ) {
            content()
        }
        bottomBar()
    }
}

@Composable
fun NoteEditorContent(
    noteDescription: String,
    onDescriptionChanged: (String) -> Unit,
    isBold: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.notezyPalette
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    TextField(
        value = noteDescription,
        onValueChange = onDescriptionChanged,
        placeholder = {
            Text(
                text = "Start typing your thoughts here...",
                color = palette.textMuted,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 14.sp
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = palette.textPrimary,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 220.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
    )
}

@Composable
fun ChecklistEditor(
    checklist: List<ChecklistItem>,
    focusRequesters: MutableMap<Int, FocusRequester>,
    onChecklistChanged: (List<ChecklistItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.notezyPalette
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }
    val bringIntoViewRequesters = remember { mutableMapOf<Int, BringIntoViewRequester>() }

    // After a new row is composed, move focus to it and scroll it into view.
    LaunchedEffect(pendingFocusIndex, checklist.size) {
        val target = pendingFocusIndex ?: return@LaunchedEffect
        if (target !in checklist.indices) return@LaunchedEffect
        // Allow the new TextField to attach before requesting focus so rapid
        // Enter presses reliably land on the freshly inserted row.
        kotlinx.coroutines.yield()
        focusRequesters[target]?.requestFocus()
        bringIntoViewRequesters[target]?.bringIntoView()
        pendingFocusIndex = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        checklist.forEachIndexed { index, item ->
            key(index) {
                val focusRequester = remember { FocusRequester() }
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(index) {
                    focusRequesters[index] = focusRequester
                    bringIntoViewRequesters[index] = bringIntoViewRequester
                }

                fun insertItemBelowCurrent() {
                    if (item.text.isBlank()) return
                    val newIndex = index + 1
                    pendingFocusIndex = newIndex
                    onChecklistChanged(
                        checklist.toMutableList().apply {
                            add(newIndex, ChecklistItem("", false))
                        }
                    )
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
                            onChecklistChanged(
                                checklist.toMutableList().also {
                                    it[index] = it[index].copy(checked = checked)
                                }
                            )
                        }
                    )

                    TextField(
                        value = item.text,
                        onValueChange = { text ->
                            onChecklistChanged(
                                checklist.toMutableList().also {
                                    it[index] = it[index].copy(text = text)
                                }
                            )
                        },
                        placeholder = {
                            Text("Enter item...", color = Color.LightGray)
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = if (item.checked) palette.textMuted else palette.textPrimary,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                            fontFamily = FontFamily(Font(R.font.inter_regular)),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusEvent { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { bringIntoViewRequester.bringIntoView() }
                                }
                            },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { insertItemBelowCurrent() },
                            onDone = { insertItemBelowCurrent() }
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
                        imageVector = Icons.Default.Clear,
                        contentDescription = "remove",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (checklist.size > 1) {
                                onChecklistChanged(
                                    checklist.toMutableList().also { it.removeAt(index) }
                                )
                            } else {
                                onChecklistChanged(listOf(ChecklistItem("", false)))
                            }
                        }
                    )
                }
            }
        }
    }
}
