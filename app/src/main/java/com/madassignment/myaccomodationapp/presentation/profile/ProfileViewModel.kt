package com.madassignment.myaccomodationapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import com.madassignment.myaccomodationapp.domain.usecase.DeleteAccountUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserProfileUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserReservationsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeUserReservations: ObserveUserReservationsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel() {

    private val uidFlow = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profile: StateFlow<UserProfile?> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeUserProfile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reservations: StateFlow<List<Reservation>> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeUserReservations(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            deleteAccountUseCase(password)
        }
    }
}
