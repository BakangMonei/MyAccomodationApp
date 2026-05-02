package com.madassignment.myaccomodationapp

import com.madassignment.myaccomodationapp.domain.usecase.SignInUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SignInUseCaseTest {

    @Test
    fun `signIn delegates to repository`() = runTest {
        val repo = mockk<com.madassignment.myaccomodationapp.domain.repository.AuthRepository>()
        coEvery { repo.signIn("a@b.c", "secret") } returns Result.success(Unit)
        val useCase = SignInUseCase(repo)
        val result = useCase("a@b.c", "secret")
        assertTrue(result.isSuccess)
    }
}
