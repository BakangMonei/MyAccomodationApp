package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val createdAt: Instant,
    val preferences: UserPreferences,
)

data class UserPreferences(
    val minPriceBwp: Double,
    val maxPriceBwp: Double,
    val locations: List<String>,
    val types: List<String>,
    val availabilityOnOrBefore: Instant?,
    val campusLatitude: Double?,
    val campusLongitude: Double?,
    val fcmToken: String?,
)

fun defaultUserPreferences(): UserPreferences = UserPreferences(
    minPriceBwp = 0.0,
    maxPriceBwp = 10_000.0,
    locations = emptyList(),
    types = emptyList(),
    availabilityOnOrBefore = null,
    campusLatitude = null,
    campusLongitude = null,
    fcmToken = null,
)
