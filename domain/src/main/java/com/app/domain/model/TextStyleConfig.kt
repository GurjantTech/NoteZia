package com.app.domain.model

data class TextStyleConfig(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSize: Int = 16,
    val fontFamily: String = "Inter-Regular"
)