package com.app.data.local


import androidx.room.Database
import androidx.room.RoomDatabase

import com.app.data.local.dao.NoteDao
import com.app.data.local.entity.NoteEntity

@Database(entities = [NoteEntity::class],
    version = 2,
    exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao() : NoteDao
}