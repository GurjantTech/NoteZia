package com.app.domain.repository

interface SecureRepository {
    suspend fun setWelcomeNotificationShown(isShown: Boolean)


}