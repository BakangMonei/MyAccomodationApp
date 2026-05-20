package com.madassignment.myaccomodationapp.domain.repository

import com.madassignment.myaccomodationapp.domain.model.AuthUser
import com.madassignment.myaccomodationapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>

    /** Synchronous snapshot for actions that must not depend on a cold auth StateFlow. */
    fun currentUser(): AuthUser?

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
    ): Result<Unit>

    suspend fun signOut()

    suspend fun deleteAccount(reAuthPassword: String): Result<Unit>
}
