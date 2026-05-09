package com.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.userSessionDataStore by preferencesDataStore(name = "user_session_preferences")

/**
 * DataStore-backed persistence for the signed-in Google user.
 *
 * Keys mirror the spec verbatim so they can be inspected via `adb shell` if
 * needed. Storing the photo URL lets the Settings screen render the avatar
 * without re-running Google Sign-In.
 */
class UserSessionStorage(private val context: Context) {

    private val isLoggedInKey = booleanPreferencesKey(KEY_IS_LOGGED_IN)
    private val userIdKey = stringPreferencesKey(KEY_USER_ID)
    private val userNameKey = stringPreferencesKey(KEY_USER_NAME)
    private val userEmailKey = stringPreferencesKey(KEY_USER_EMAIL)
    private val userPhotoKey = stringPreferencesKey(KEY_USER_PHOTO_URL)
    private val lastSyncKey = longPreferencesKey(KEY_LAST_SYNC_TIME)

    val currentUser: Flow<UserProfile?> = context.userSessionDataStore.data.map { prefs ->
        val loggedIn = prefs[isLoggedInKey] ?: false
        if (!loggedIn) return@map null
        val userId = prefs[userIdKey].orEmpty()
        if (userId.isBlank()) return@map null
        UserProfile(
            userId = userId,
            name = prefs[userNameKey].orEmpty(),
            email = prefs[userEmailKey].orEmpty(),
            photoUrl = prefs[userPhotoKey].orEmpty(),
            lastSyncTime = prefs[lastSyncKey] ?: 0L
        )
    }

    suspend fun snapshot(): UserProfile? = currentUser.firstOrNull()

    suspend fun saveUser(profile: UserProfile) {
        context.userSessionDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[userIdKey] = profile.userId
            prefs[userNameKey] = profile.name
            prefs[userEmailKey] = profile.email
            prefs[userPhotoKey] = profile.photoUrl
        }
    }

    suspend fun updateLastSyncTime(timestamp: Long) {
        context.userSessionDataStore.edit { prefs ->
            prefs[lastSyncKey] = timestamp
        }
    }

    suspend fun clear() {
        context.userSessionDataStore.edit { prefs -> prefs.clear() }
    }

    companion object {
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_PHOTO_URL = "user_photo_url"
        const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }
}
