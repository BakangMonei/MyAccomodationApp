package com.madassignment.myaccomodationapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.domain.model.defaultUserPreferences
import com.madassignment.myaccomodationapp.domain.usecase.CancelReservationUseCase
import com.madassignment.myaccomodationapp.domain.usecase.DeleteAccountUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserProfileUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserReservationsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeUserReservations: ObserveUserReservationsUseCase,
    private val cancelReservationUseCase: CancelReservationUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel() {

    private val authUser = observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val uidFlow = authUser
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isSignedIn: StateFlow<Boolean> = uidFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val observedProfile: StateFlow<UserProfile?> = uidFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else observeUserProfile(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile: StateFlow<UserProfile?> = combine(observedProfile, authUser) { profile, auth ->
        profile ?: auth?.let { user ->
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.email?.substringBefore("@").orEmpty(),
                role = UserRole.Student,
                createdAt = java.time.Instant.now(),
                preferences = defaultUserPreferences(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeReservations: StateFlow<List<Reservation>> = uidFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else observeUserReservations(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _cancellingIds = MutableStateFlow<Set<String>>(emptySet())
    val cancellingIds: StateFlow<Set<String>> = _cancellingIds.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun cancelReservation(reservationId: String) {
        val uid = uidFlow.value ?: return
        if (_cancellingIds.value.contains(reservationId)) return
        viewModelScope.launch {
            _cancellingIds.value = _cancellingIds.value + reservationId
            cancelReservationUseCase(reservationId, uid).fold(
                onSuccess = {
                    _events.emit("Reservation cancelled — listing is available again.")
                },
                onFailure = { e ->
                    _events.emit(e.message ?: "Could not cancel reservation.")
                },
            )
            _cancellingIds.value = _cancellingIds.value - reservationId
        }
    }

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            deleteAccountUseCase(password)
        }
    }
}
