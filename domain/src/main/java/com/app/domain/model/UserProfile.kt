package com.app.domain.model

/**
 * Authenticated user profile sourced from Firebase Authentication.
 *
 * [userId] is the Firebase Auth UID — used as the Firestore document id under
 * `users/{uid}` and for local note ownership.
 */
data class UserProfile(
    val userId: String,
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false,
    val lastSyncTime: Long = 0L
)
