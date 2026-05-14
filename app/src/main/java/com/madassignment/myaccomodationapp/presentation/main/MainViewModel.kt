package com.madassignment.myaccomodationapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveTotalChatUnreadUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeTotalChatUnread: ObserveTotalChatUnreadUseCase,
) : ViewModel() {

    private val uidFlow = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile: StateFlow<UserProfile?> = uidFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else observeUserProfile(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val unreadChats: StateFlow<Int> = uidFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(0) else observeTotalChatUnread(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
