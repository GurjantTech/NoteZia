package com.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.app.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

class ThemeRepositoryImpl(
    private val context: Context
) : ThemeRepository {

    private val darkThemeKey = booleanPreferencesKey("dark_theme_enabled")

    override fun isDarkThemeEnabled(): Flow<Boolean> {
        return context.themeDataStore.data.map { preferences ->
            preferences[darkThemeKey] ?: false
        }
    }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[darkThemeKey] = enabled
        }
    }
}
