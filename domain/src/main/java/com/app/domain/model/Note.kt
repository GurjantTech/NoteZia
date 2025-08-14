package com.app.domain.model

data class Note(
    var noteId: String? = "",
    var title: String? = "",
    var description: String? = "",
    var timeStamp: String? = "",
    var contentJson: String? = "",
    var noteType: String? = ""
)