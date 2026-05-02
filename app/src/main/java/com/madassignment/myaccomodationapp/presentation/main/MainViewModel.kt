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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profile: StateFlow<UserProfile?> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeUserProfile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val unreadChats: StateFlow<Int> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeTotalChatUnread(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
