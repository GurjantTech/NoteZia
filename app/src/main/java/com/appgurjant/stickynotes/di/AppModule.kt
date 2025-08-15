package com.appgurjant.stickynotes.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.data.local.NoteDatabase
import com.app.data.local.dao.NoteDao
import com.app.data.local.entity.NoteEntity
import com.app.data.repository.NoteRepositoryImpl
import com.app.domain.repository.NoteRepository
import com.app.domain.usecase.AddNoteUseCase
import com.app.domain.usecase.AllNoteUseCase
import com.app.domain.usecase.DeleteNoteUseCase
import com.app.domain.usecase.GetNoteDetailFromLocalUseCase
import com.app.domain.usecase.UpdateNoteDetailFromLocalUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    private val Migration_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Add columns only if they don't exist
                val cursor = database.query("PRAGMA table_info(notes)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if (!existingColumns.contains("contentJson")) {
                    database.execSQL("ALTER TABLE notes ADD COLUMN contentJson TEXT DEFAULT NULL")
                }

                if (!existingColumns.contains("noteType")) {
                    database.execSQL("ALTER TABLE notes ADD COLUMN noteType TEXT DEFAULT NULL")
                }

            } catch (e: Exception) {
                Log.e("Migration", "Error during migration", e)
                throw e // Let Room handle fallback if migration fails
            }
        }
    }
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoteDatabase {
        return Room.databaseBuilder(context,
            NoteDatabase::class.java, // <-- Directly accessed from Data module
            "notezy_db")
            .addMigrations(Migration_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNoteDao(db: NoteDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository = NoteRepositoryImpl(noteDao)

    @Provides
    fun addNoteUseCase(noteRepository: NoteRepository) = AddNoteUseCase(noteRepository)
    @Provides
    fun allNoteUseCase(noteRepository: NoteRepository) = AllNoteUseCase(noteRepository)
    @Provides
    fun getNoteDetailFromLocalUseCase(noteRepository: NoteRepository) = GetNoteDetailFromLocalUseCase(noteRepository)
    @Provides
    fun updateNoteDetailFromLocalUseCase(noteRepository: NoteRepository) =
        UpdateNoteDetailFromLocalUseCase(noteRepository)

    @Provides
    fun deleteNoteUseCase(noteRepository: NoteRepository) = DeleteNoteUseCase(noteRepository)



}