package com.madassignment.myaccomodationapp.domain.repository

import com.madassignment.myaccomodationapp.domain.model.UserPreferences
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getProfile(uid: String): Result<UserProfile>

    fun observeProfile(uid: String): Flow<UserProfile?>

    suspend fun updatePreferences(uid: String, preferences: UserPreferences): Result<Unit>
}
