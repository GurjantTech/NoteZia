package com.appgurjant.stickynotes.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.app.data.auth.FirebaseAuthManager
import com.app.data.auth.UserSessionStorage
import com.app.data.local.NoteDatabase
import com.app.data.local.NoteDatabaseFiles
import com.app.data.local.dao.NoteDao
import com.app.data.repository.AuthRepositoryImpl
import com.app.data.repository.FirestoreRepositoryImpl
import com.app.data.repository.NoteRepositoryImpl
import com.app.data.repository.OnboardingRepositoryImpl
import com.app.data.repository.SecureRepositoryImpl
import com.app.data.repository.SyncRepositoryImpl
import com.app.data.repository.ThemeRepositoryImpl
import com.app.data.security.SecureStorage
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository
import com.app.domain.repository.NoteRepository
import com.app.domain.repository.OnboardingRepository
import com.app.domain.repository.SecureRepository
import com.app.domain.repository.SyncRepository
import com.app.domain.repository.SyncScheduler
import com.app.domain.repository.ThemeRepository
import com.app.domain.usecase.ActivateCloudSyncUseCase
import com.app.domain.usecase.AddNoteUseCase
import com.app.domain.usecase.AllNoteUseCase
import com.app.domain.usecase.CompletePhoneAutoSignInUseCase
import com.app.domain.usecase.DeleteNoteUseCase
import com.app.domain.usecase.DismissSyncBannerUseCase
import com.app.domain.usecase.GetNoteDetailFromLocalUseCase
import com.app.domain.usecase.GetThemeModeUseCase
import com.app.domain.usecase.HasPendingNotesUseCase
import com.app.domain.usecase.IsOnboardingCompletedUseCase
import com.app.domain.usecase.ObserveCurrentUserUseCase
import com.app.domain.usecase.ObserveSyncBannerDismissedUseCase
import com.app.domain.usecase.RequestSyncUseCase
import com.app.domain.usecase.SaveUserProfileUseCase
import com.app.domain.usecase.SendPhoneVerificationCodeUseCase
import com.app.domain.usecase.SetOnboardingCompletedUseCase
import com.app.domain.usecase.SetThemeModeUseCase
import com.app.domain.usecase.SignInWithGoogleUseCase
import com.app.domain.usecase.SignOutUseCase
import com.app.domain.usecase.SyncPendingNotesUseCase
import com.app.domain.usecase.UpdateNoteDetailFromLocalUseCase
import com.app.domain.usecase.UploadNoteUseCase
import com.app.domain.usecase.VerifyPhoneCodeUseCase
import com.app.data.local.CoinStorage
import com.app.data.repository.CoinRepositoryImpl
import com.app.domain.repository.CoinRepository
import com.app.domain.usecase.ActivatePremiumTrialUseCase
import com.app.domain.usecase.AddRewardedAdCoinsUseCase
import com.app.domain.usecase.ObserveCoinStateUseCase
import com.app.domain.usecase.UnlockSecurityUseCase
import com.appgurjant.stickynotes.sync.WorkManagerSyncScheduler
import com.google.firebase.auth.FirebaseAuth
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
     * existing rows are flagged as pending (`0`) so they upload automatically
     * after the user signs in, never losing local data.
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

    /**
     * Adds the `isDeleted` tombstone column for offline-safe deletion sync.
     * Existing rows default to 0 (active); the sync worker will pick up new
     * tombstones, replicate the deletion to Firestore, and hard-delete locally.
     */
    private val Migration_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                val cursor = database.query("PRAGMA table_info(notes)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if (!existingColumns.contains("isDeleted")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                    )
                }
            } catch (e: Exception) {
                Log.e("Migration", "Error during 4→5 migration", e)
                throw e
            }
        }
    }

    /** Adds Firebase uid ownership so notes never leak across account switches. */
    private val Migration_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                val cursor = database.query("PRAGMA table_info(notes)")
                val existingColumns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if (!existingColumns.contains("ownerUserId")) {
                    database.execSQL(
                        "ALTER TABLE notes ADD COLUMN ownerUserId TEXT DEFAULT NULL"
                    )
                }
            } catch (e: Exception) {
                Log.e("Migration", "Error during 5→6 migration", e)
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
            .addMigrations(Migration_1_2, Migration_2_3, Migration_3_4, Migration_4_5, Migration_5_6)
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
    fun provideNoteRepository(
        noteDao: NoteDao,
        authRepository: AuthRepository
    ): NoteRepository = NoteRepositoryImpl(noteDao, authRepository)

    @Provides
    @Singleton
    fun provideThemeRepository(@ApplicationContext context: Context): ThemeRepository {
        return ThemeRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideOnboardingRepository(@ApplicationContext context: Context): OnboardingRepository {
        return OnboardingRepositoryImpl(context)
    }

    @Provides
    fun provideIsOnboardingCompletedUseCase(
        onboardingRepository: OnboardingRepository
    ) = IsOnboardingCompletedUseCase(onboardingRepository)

    @Provides
    fun provideSetOnboardingCompletedUseCase(
        onboardingRepository: OnboardingRepository
    ) = SetOnboardingCompletedUseCase(onboardingRepository)

    @Provides
    fun addNoteUseCase(
        noteRepository: NoteRepository,
        authRepository: AuthRepository,
        requestSyncUseCase: RequestSyncUseCase
    ) = AddNoteUseCase(noteRepository, authRepository, requestSyncUseCase)
    @Provides
    fun allNoteUseCase(noteRepository: NoteRepository) = AllNoteUseCase(noteRepository)
    @Provides
    fun getNoteDetailFromLocalUseCase(noteRepository: NoteRepository) = GetNoteDetailFromLocalUseCase(noteRepository)
    @Provides
    fun updateNoteDetailFromLocalUseCase(
        noteRepository: NoteRepository,
        authRepository: AuthRepository,
        requestSyncUseCase: RequestSyncUseCase
    ) = UpdateNoteDetailFromLocalUseCase(noteRepository, authRepository, requestSyncUseCase)

    @Provides
    fun deleteNoteUseCase(
        noteRepository: NoteRepository,
        authRepository: AuthRepository,
        requestSyncUseCase: RequestSyncUseCase
    ) = DeleteNoteUseCase(noteRepository, authRepository, requestSyncUseCase)

    @Provides
    fun provideGetThemeModeUseCase(themeRepository: ThemeRepository): GetThemeModeUseCase {
        return GetThemeModeUseCase(themeRepository)
    }

    @Provides
    fun provideSetThemeModeUseCase(themeRepository: ThemeRepository): SetThemeModeUseCase {
        return SetThemeModeUseCase(themeRepository)
    }


    // ============================================================
    // Cloud-sync (Firebase Auth + Firestore + WorkManager)
    // ============================================================

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuthManager(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth
    ): FirebaseAuthManager = FirebaseAuthManager(context, firebaseAuth)

    @Provides
    @Singleton
    fun provideUserSessionStorage(@ApplicationContext context: Context): UserSessionStorage =
        UserSessionStorage(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthManager: FirebaseAuthManager,
        sessionStorage: UserSessionStorage
    ): AuthRepository = AuthRepositoryImpl(firebaseAuthManager, sessionStorage)

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
        firestoreRepository: FirestoreRepository
    ): SyncRepository = SyncRepositoryImpl(noteRepository, firestoreRepository)

    // ---- cloud-sync use cases ----

    @Provides
    fun provideActivateCloudSyncUseCase(
        noteRepository: NoteRepository,
        requestSyncUseCase: RequestSyncUseCase
    ) = ActivateCloudSyncUseCase(noteRepository, requestSyncUseCase)

    @Provides
    fun provideSignInWithGoogleUseCase(
        authRepository: AuthRepository,
        firestoreRepository: FirestoreRepository,
        activateCloudSyncUseCase: ActivateCloudSyncUseCase
    ) = SignInWithGoogleUseCase(authRepository, firestoreRepository, activateCloudSyncUseCase)

    @Provides
    fun provideSignOutUseCase(
        authRepository: AuthRepository,
        syncRepository: SyncRepository,
        noteRepository: NoteRepository,
        syncScheduler: SyncScheduler
    ) = SignOutUseCase(authRepository, syncRepository, noteRepository, syncScheduler)

    @Provides
    fun provideSendPhoneVerificationCodeUseCase(
        authRepository: AuthRepository
    ) = SendPhoneVerificationCodeUseCase(authRepository)

    @Provides
    fun provideVerifyPhoneCodeUseCase(
        authRepository: AuthRepository,
        firestoreRepository: FirestoreRepository,
        activateCloudSyncUseCase: ActivateCloudSyncUseCase
    ) = VerifyPhoneCodeUseCase(authRepository, firestoreRepository, activateCloudSyncUseCase)

    @Provides
    fun provideCompletePhoneAutoSignInUseCase(
        firestoreRepository: FirestoreRepository,
        activateCloudSyncUseCase: ActivateCloudSyncUseCase
    ) = CompletePhoneAutoSignInUseCase(firestoreRepository, activateCloudSyncUseCase)

    @Provides
    fun provideObserveSyncBannerDismissedUseCase(authRepository: AuthRepository) =
        ObserveSyncBannerDismissedUseCase(authRepository)

    @Provides
    fun provideDismissSyncBannerUseCase(authRepository: AuthRepository) =
        DismissSyncBannerUseCase(authRepository)

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
        syncScheduler: SyncScheduler,
        syncPendingNotesUseCase: SyncPendingNotesUseCase
    ) = RequestSyncUseCase(authRepository, syncScheduler, syncPendingNotesUseCase)

    @Provides
    fun provideObserveCurrentUserUseCase(authRepository: AuthRepository) =
        ObserveCurrentUserUseCase(authRepository)

    // ============================================================
    // Coin economy (rewarded ads → Security unlock → ad-free)
    // ============================================================

    @Provides
    @Singleton
    fun provideCoinStorage(@ApplicationContext context: Context): CoinStorage =
        CoinStorage(context)

    @Provides
    @Singleton
    fun provideCoinRepository(coinStorage: CoinStorage): CoinRepository =
        CoinRepositoryImpl(coinStorage)

    @Provides
    fun provideObserveCoinStateUseCase(coinRepository: CoinRepository) =
        ObserveCoinStateUseCase(coinRepository)

    @Provides
    fun provideAddRewardedAdCoinsUseCase(coinRepository: CoinRepository) =
        AddRewardedAdCoinsUseCase(coinRepository)

    @Provides
    fun provideUnlockSecurityUseCase(coinRepository: CoinRepository) =
        UnlockSecurityUseCase(coinRepository)

    @Provides
    fun provideActivatePremiumTrialUseCase(coinRepository: CoinRepository) =
        ActivatePremiumTrialUseCase(coinRepository)
}