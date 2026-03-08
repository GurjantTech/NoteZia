package com.app.domain.model

data class Note(
    var noteId: String? = "",
    var title: String? = "",
    var description: String? = null,
    var timeStamp: String? = "",
    var contentJson: String? = "",
    var noteType: String? = "",
    var textStyleConfig: TextStyleConfig? = TextStyleConfig()
)