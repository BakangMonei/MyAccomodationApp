package com.madassignment.myaccomodationapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.usecase.ObserveListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeListing: ObserveListingUseCase,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    val listing: StateFlow<Listing?> = observeListing(listingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
