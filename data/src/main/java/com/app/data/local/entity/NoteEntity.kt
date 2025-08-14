package com.app.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int=0,
    val title:String?="",
    val description:String?="",
    val timeStamp:String?="",
    val contentJson:String?="",
    val noteType:String?="",

)

