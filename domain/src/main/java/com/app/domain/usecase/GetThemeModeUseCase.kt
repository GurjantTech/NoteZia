package com.app.domain.usecase

import com.app.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

class GetThemeModeUseCase(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isDarkThemeEnabled()
}
