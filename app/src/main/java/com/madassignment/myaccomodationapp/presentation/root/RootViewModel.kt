package com.madassignment.myaccomodationapp.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.AuthUser
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
) : ViewModel() {

    val authUser: StateFlow<AuthUser?> = observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
