package com.madassignment.myaccomodationapp.domain.usecase

import com.madassignment.myaccomodationapp.domain.model.UserPreferences
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import com.madassignment.myaccomodationapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    operator fun invoke(uid: String): Flow<UserProfile?> = userRepository.observeProfile(uid)
}

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(uid: String): Result<UserProfile> = userRepository.getProfile(uid)
}

class SaveUserPreferencesUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(uid: String, preferences: UserPreferences): Result<Unit> =
        userRepository.updatePreferences(uid, preferences)
}
