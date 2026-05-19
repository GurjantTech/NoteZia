package com.app.domain.model

/**
 * Authenticated user profile sourced from Google Sign-In.
 *
 * [userId] is the Google account "sub" claim — used directly as the Firestore
 * document ID under `users/{userId}`. We deliberately do not pull anything from
 * Firebase Authentication.
 */
data class UserProfile(
    val userId: String,
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false,
    val lastSyncTime: Long = 0L
)
