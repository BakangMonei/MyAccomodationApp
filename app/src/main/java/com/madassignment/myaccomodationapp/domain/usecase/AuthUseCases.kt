package com.madassignment.myaccomodationapp.domain.usecase

import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        authRepository.signIn(email, password)
}

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
    ): Result<Unit> = authRepository.signUp(email, password, displayName, role)
}

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(password: String): Result<Unit> =
        authRepository.deleteAccount(password)
}
