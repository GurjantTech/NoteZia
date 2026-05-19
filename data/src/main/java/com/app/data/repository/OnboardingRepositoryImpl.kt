package com.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

class OnboardingRepositoryImpl(
    private val context: Context
) : OnboardingRepository {

    private val onboardingCompletedKey = booleanPreferencesKey("is_onboarding_completed")

    override fun isOnboardingCompleted(): Flow<Boolean> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[onboardingCompletedKey] ?: false
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }
}
