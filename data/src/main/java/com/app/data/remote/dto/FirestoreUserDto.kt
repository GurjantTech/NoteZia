package com.app.data.remote.dto

import com.app.domain.model.UserProfile

/**
 * Wire format for `users/{userId}`.
 *
 * Defaults are required by Firestore's reflective deserializer: every field
 * must be either a `var` with a default or a no-arg constructor.
 */
data class FirestoreUserDto(
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = 0L,
    val isPremium: Boolean = false,
    val lastSyncTime: Long = 0L
)

fun UserProfile.toFirestoreDto() = FirestoreUserDto(
    name = name,
    email = email,
    photoUrl = photoUrl,
    phoneNumber = phoneNumber,
    createdAt = createdAt,
    isPremium = isPremium,
    lastSyncTime = lastSyncTime
)
