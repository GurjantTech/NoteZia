package com.appgurjant.stickynotes.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.app.data.auth.GoogleAuthManager
import com.app.data.auth.UserSessionStorage
import com.app.data.local.NoteDatabase
import com.app.data.local.NoteDatabaseFiles
import com.app.data.local.dao.NoteDao
import com.app.data.repository.AuthRepositoryImpl
import com.app.data.repository.FirestoreRepositoryImpl
import com.app.data.repository.NoteRepositoryImpl
import com.app.data.repository.SecureRepositoryImpl
import com.app.data.repository.SyncRepositoryImpl
import com.app.data.repository.ThemeRepositoryImpl
import com.app.data.security.SecureStorage
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository
import com.app.domain.repository.NoteRepository
import com.app.domain.repository.SecureRepository
import com.app.domain.repository.SyncRepository
import com.app.domain.repository.SyncScheduler
import com.app.domain.repository.ThemeRepository
import com.app.domain.usecase.AddNoteUseCase
import com.app.domain.usecase.AllNoteUseCase
import com.app.domain.usecase.DeleteNoteUseCase
import com.app.domain.usecase.GetNoteDetailFromLocalUseCase
import com.app.domain.usecase.GetThemeModeUseCase
import com.app.domain.usecase.ObserveCurrentUserUseCase
import com.app.domain.usecase.RequestSyncUseCase
import com.app.domain.usecase.SaveUserProfileUseCase
import com.app.domain.usecase.SetThemeModeUseCase
import com.app.domain.usecase.SignInWithGoogleUseCase
import com.app.domain.usecase.SignOutUseCase
import com.app.domain.usecase.HasPendingNotesUseCase
import com.app.domain.usecase.SyncPendingNotesUseCase
import com.app.domain.usecase.UpdateNoteDetailFromLocalUseCase
import com.app.domain.usecase.UploadNoteUseCase
import com.appgurjant.stickynotes.sync.WorkManagerSyncScheduler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore
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

    /**
     * Adds the `isSync` column introduced for Google-account cloud sync. All
     * existing rows are flagged as pending (`0`) so they upload on the user's
     * first sign-in, never losing local data.
     */
    private val Migration_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                val cursor = database.query("PRAGMA table_info(notes)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if (!existingColumns.contains("isSync")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN isSync INTEGER NOT NULL DEFAULT 0"
                    )
                }
            } catch (e: Exception) {
                Log.e("Migration", "Error during 2→3 migration", e)
                throw e
            }
        }
    }

    /**
     * Adds epoch millis for reliable merge ordering, optional reminder, and
     * persisted rich-text style JSON for parity with Firestore / cloud sync.
     */
    private val Migration_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                val cursor = database.query("PRAGMA table_info(notes)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if (!existingColumns.contains("createdAtMillis")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if (!existingColumns.contains("updatedAtMillis")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0"
                    )
                }
                if (!existingColumns.contains("reminderAtMillis")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN reminderAtMillis INTEGER"
                    )
                }
                if (!existingColumns.contains("textStyleJson")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN textStyleJson TEXT DEFAULT NULL"
                    )
                }
            } catch (e: Exception) {
                Log.e("Migration", "Error during 3→4 migration", e)
                throw e
            }
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoteDatabase {
        NoteDatabaseFiles.preparePlainSqliteDatabase(context)
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            NoteDatabaseFiles.DATABASE_NAME
        )
            .addMigrations(Migration_1_2, Migration_2_3, Migration_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }


//==========================================================================
    // In AppModule.kt
    @Module
    @InstallIn(SingletonComponent::class)
    object SecurityModule {

        @Provides
        @Singleton
        fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
            return SecureStorage(context)
        }

        @Provides
        @Singleton
        fun provideSecureRepository(secureStorage: SecureStorage): SecureRepository {
            return SecureRepositoryImpl(secureStorage)
        }
    }
    //==========================================================================

    @Provides
    fun provideNoteDao(db: NoteDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository = NoteRepositoryImpl(noteDao)

    @Provides
    @Singleton
    fun provideThemeRepository(@ApplicationContext context: Context): ThemeRepository {
        return ThemeRepositoryImpl(context)
    }

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

    @Provides
    fun provideGetThemeModeUseCase(themeRepository: ThemeRepository): GetThemeModeUseCase {
        return GetThemeModeUseCase(themeRepository)
    }

    @Provides
    fun provideSetThemeModeUseCase(themeRepository: ThemeRepository): SetThemeModeUseCase {
        return SetThemeModeUseCase(themeRepository)
    }


    // ============================================================
    // Cloud-sync (Google Sign-In + Firestore + WorkManager)
    // ============================================================

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * `GoogleSignInClient` is only used for [GoogleSignInClient.signOut] —
     * the actual sign-in flow goes through Credential Manager via
     * [GoogleAuthManager]. We still need a configured client so the user's
     * account selection is properly cleared on logout.
     */
    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GoogleAuthManager.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthManager(@ApplicationContext context: Context): GoogleAuthManager =
        GoogleAuthManager(context)

    @Provides
    @Singleton
    fun provideUserSessionStorage(@ApplicationContext context: Context): UserSessionStorage =
        UserSessionStorage(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        googleAuthManager: GoogleAuthManager,
        sessionStorage: UserSessionStorage,
        signInClient: GoogleSignInClient
    ): AuthRepository = AuthRepositoryImpl(googleAuthManager, sessionStorage, signInClient)

    @Provides
    @Singleton
    fun provideFirestoreRepository(
        firestore: FirebaseFirestore,
        sessionStorage: UserSessionStorage
    ): FirestoreRepository = FirestoreRepositoryImpl(firestore, sessionStorage)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideSyncScheduler(workManager: WorkManager): SyncScheduler =
        WorkManagerSyncScheduler(workManager)

    @Provides
    @Singleton
    fun provideSyncRepository(
        noteRepository: NoteRepository,
        firestoreRepository: FirestoreRepository,
        authRepository: AuthRepository,
        syncScheduler: SyncScheduler
    ): SyncRepository = SyncRepositoryImpl(
        noteRepository,
        firestoreRepository,
        authRepository,
        syncScheduler
    )

    // ---- cloud-sync use cases ----

    @Provides
    fun provideSignInWithGoogleUseCase(
        authRepository: AuthRepository,
        firestoreRepository: FirestoreRepository,
        syncScheduler: SyncScheduler
    ) = SignInWithGoogleUseCase(authRepository, firestoreRepository, syncScheduler)

    @Provides
    fun provideSignOutUseCase(
        authRepository: AuthRepository,
        syncScheduler: SyncScheduler
    ) = SignOutUseCase(authRepository, syncScheduler)

    @Provides
    fun provideSaveUserProfileUseCase(firestoreRepository: FirestoreRepository) =
        SaveUserProfileUseCase(firestoreRepository)

    @Provides
    fun provideSyncPendingNotesUseCase(
        authRepository: AuthRepository,
        syncRepository: SyncRepository
    ) = SyncPendingNotesUseCase(authRepository, syncRepository)

    @Provides
    fun provideHasPendingNotesUseCase(syncRepository: SyncRepository) =
        HasPendingNotesUseCase(syncRepository)

    @Provides
    fun provideUploadNoteUseCase(
        authRepository: AuthRepository,
        firestoreRepository: FirestoreRepository
    ) = UploadNoteUseCase(authRepository, firestoreRepository)

    @Provides
    fun provideRequestSyncUseCase(
        authRepository: AuthRepository,
        syncScheduler: SyncScheduler
    ) = RequestSyncUseCase(authRepository, syncScheduler)

    @Provides
    fun provideObserveCurrentUserUseCase(authRepository: AuthRepository) =
        ObserveCurrentUserUseCase(authRepository)
}