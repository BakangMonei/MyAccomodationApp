package com.madassignment.myaccomodationapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.domain.usecase.SignInUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            signInUseCase(email, password)
                .onSuccess { _events.emit(AuthEvent.SignedIn) }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Sign-in failed") }
            if (_state.value is AuthUiState.Loading) _state.value = AuthUiState.Idle
        }
    }

    fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
    ) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            signUpUseCase(email, password, displayName, role)
                .onSuccess { _events.emit(AuthEvent.SignedIn) }
                .onFailure { _state.value = AuthUiState.Error(it.message ?: "Registration failed") }
            if (_state.value is AuthUiState.Loading) _state.value = AuthUiState.Idle
        }
    }

    fun consumeError() {
        if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle
    }
}

sealed interface AuthEvent {
    data object SignedIn : AuthEvent
}
