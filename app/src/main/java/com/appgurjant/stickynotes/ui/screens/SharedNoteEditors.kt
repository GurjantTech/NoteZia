package com.appgurjant.stickynotes.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.appgurjant.stickynotes.R
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.NoteEditorContent(
    noteDescription: String,
    onDescriptionChanged: (String) -> Unit,
    isBold: Boolean
) {
    val palette = MaterialTheme.notezyPalette
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
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    )
}

@Composable
fun ColumnScope.ChecklistEditor(
    checklist: List<ChecklistItem>,
    focusRequesters: MutableMap<Int, FocusRequester>,
    onChecklistChanged: (List<ChecklistItem>) -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    LaunchedEffect(checklist.size) {
        if (checklist.isNotEmpty()) {
            focusRequesters[checklist.lastIndex]?.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 10.dp)
    ) {
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
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (index == checklist.lastIndex && item.text.isNotBlank()) {
                                onChecklistChanged(checklist + ChecklistItem("", false))
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
                    ),
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
