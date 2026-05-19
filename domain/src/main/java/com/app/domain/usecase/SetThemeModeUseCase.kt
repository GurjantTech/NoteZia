package com.app.domain.usecase

import com.app.domain.repository.ThemeRepository

class SetThemeModeUseCase(private val repository: ThemeRepository) {
    suspend operator fun invoke(isDark: Boolean) = repository.setDarkThemeEnabled(isDark)
}
