package com.appgurjant.stickynotes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.domain.model.TextStyleConfig


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textFormatting: TextStyleConfig = TextStyleConfig(),
    onFormatChange: (TextStyleConfig) -> Unit = {}
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value))
    }

    LaunchedEffect(value){
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
                    fontWeight = if (textFormatting.isBold)
                        FontWeight.Bold else FontWeight.Normal,

                    fontStyle = if (textFormatting.isItalic)
                        FontStyle.Italic else FontStyle.Normal,

                    textDecoration = if (textFormatting.isUnderline)
                        TextDecoration.Underline else TextDecoration.None,

                    fontSize = textFormatting.fontSize.sp,
                    lineHeight = (textFormatting.fontSize * 1.5).sp,

                    fontFamily = getFontFamily(textFormatting.fontFamily)
                )
            )
        )
        
        // Formatting toolbar - always visible at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .background(Color.White)
                .border(0.1.dp, Color.Gray,RoundedCornerShape(20.dp))

        ) {
            FormattingToolbar(
                textFormatting = textFormatting,
                onFormatChange ={
                    onFormatChange(it)
                } ,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
fun getFontFamily(fontName: String): FontFamily {
    return when (fontName) {
        "Inter-Regular" -> FontFamily.Default
        "Inter-Bold" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

@Composable
fun FormattingToolbar(
    textFormatting: TextStyleConfig,
    onFormatChange: (TextStyleConfig) -> Unit,
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
            checked = textFormatting.isUnderline,
            onCheckedChange = {
                onFormatChange(textFormatting.copy(isUnderline = it))
            },
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (textFormatting.isUnderline) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
        // Theme Color
//        // Italic button
//        IconToggleButton(
//            checked = textFormatting.isItalic,
//            onCheckedChange = {
////               Toast.makeText(Context(), "Color lens", Toast.LENGTH_SHORT).show()
//            },
//            modifier = Modifier
//                .size(48.dp)
//                .padding(4.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.ColorLens,
//                contentDescription = "Color lens",
//                tint = if (textFormatting.isItalic) MaterialTheme.colorScheme.primary else Color.Gray
//            )
//        }

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
