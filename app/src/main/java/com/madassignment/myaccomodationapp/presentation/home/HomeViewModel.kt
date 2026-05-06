package com.madassignment.myaccomodationapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.model.defaultUserPreferences
import com.madassignment.myaccomodationapp.domain.model.toListingFilters
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveFilteredListingsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserProfileUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val saveUserPreferences: SaveUserPreferencesUseCase,
    private val observeFilteredListings: ObserveFilteredListingsUseCase,
) : ViewModel() {

    private val authUid = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val userProfile = authUid
        .filterNotNull()
        .flatMapLatest { uid -> observeUserProfile(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _appliedFilters = MutableStateFlow(
        ListingFilters(0.0, 10_000.0, emptyList(), emptyList(), null),
    )
    val appliedFilters: StateFlow<ListingFilters> = _appliedFilters.asStateFlow()

    init {
        viewModelScope.launch {
            userProfile.collect { profile ->
                profile?.let { _appliedFilters.value = it.preferences.toListingFilters() }
            }
        }
    }

    /**
     * Null means the first snapshot has not arrived yet; empty list is a valid result.
     */
    val listings: StateFlow<List<Listing>?> =
        _appliedFilters
            .flatMapLatest { filters ->
                observeFilteredListings(filters)
                    .map<List<Listing>, List<Listing>?> { it }
                    .onStart { emit(null) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun applyFilters(filters: ListingFilters) {
        _appliedFilters.value = filters
        val uid = authUid.value ?: return
        viewModelScope.launch {
            val current = userProfile.value?.preferences ?: defaultUserPreferences()
            saveUserPreferences(
                uid,
                current.copy(
                    minPriceBwp = filters.minPrice,
                    maxPriceBwp = filters.maxPrice,
                    locations = filters.locations,
                    types = filters.types,
                    availabilityOnOrBefore = filters.availabilityOnOrBefore,
                ),
            )
        }
    }
}
