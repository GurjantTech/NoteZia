package com.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun isDarkThemeEnabled(): Flow<Boolean>
    suspend fun setDarkThemeEnabled(enabled: Boolean)
}
