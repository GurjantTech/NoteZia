package com.appgurjant.stickynotes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appgurjant.stickynotes.R

data class TextFormatting(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val fontSize: Int = 16,
    val fontFamily: FontFamily = FontFamily(Font(R.font.inter_regular))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textFormatting: TextFormatting = TextFormatting(),
    onFormatChange: (TextFormatting) -> Unit = {}
) {
    var textFieldValue by remember(value) { 
        mutableStateOf(TextFieldValue(text = value)) 
    }
    
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(text = value)
        }
    }

    val view = LocalView.current
    val isKeyboardOpen by remember {
        derivedStateOf {
            val insets = ViewCompat.getRootWindowInsets(view)
            insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        // Text input field with formatting
        TextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onValueChange(it.text)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.merge(
                TextStyle(
                    fontWeight = if (textFormatting.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (textFormatting.isItalic) 
                        androidx.compose.ui.text.font.FontStyle.Italic 
                        else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = if (textFormatting.isUnderlined) 
                        TextDecoration.Underline else TextDecoration.None,
                    fontSize = textFormatting.fontSize.sp,
                    fontFamily = textFormatting.fontFamily,
                    lineHeight = (textFormatting.fontSize * 1.5).sp
                )
            )
        )
        
        // Formatting toolbar - always visible at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            FormattingToolbar(
                textFormatting = textFormatting,
                onFormatChange = onFormatChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FormattingToolbar(
    textFormatting: TextFormatting,
    onFormatChange: (TextFormatting) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Bold button
        IconToggleButton(
            checked = textFormatting.isBold,
            onCheckedChange = {
                onFormatChange(textFormatting.copy(isBold = it))
            },
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatBold,
                contentDescription = "Bold",
                tint = if (textFormatting.isBold) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        // Italic button
        IconToggleButton(
            checked = textFormatting.isItalic,
            onCheckedChange = {
                onFormatChange(textFormatting.copy(isItalic = it))
            },
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                tint = if (textFormatting.isItalic) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        // Underline button
        IconToggleButton(
            checked = textFormatting.isUnderlined,
            onCheckedChange = {
                onFormatChange(textFormatting.copy(isUnderlined = it))
            },
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (textFormatting.isUnderlined) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Font size controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    if (textFormatting.fontSize > 12) {
                        onFormatChange(textFormatting.copy(fontSize = textFormatting.fontSize - 2))
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = "Decrease font size",
                    tint = Color.Gray
                )
            }
            
            Text(
                text = "${textFormatting.fontSize}sp",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
            
            IconButton(
                onClick = {
                    if (textFormatting.fontSize < 32) {
                        onFormatChange(textFormatting.copy(fontSize = textFormatting.fontSize + 2))
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Increase font size",
                    tint = Color.Gray
                )
            }
        }
    }
}
