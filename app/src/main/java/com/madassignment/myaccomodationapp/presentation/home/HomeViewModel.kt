package com.madassignment.myaccomodationapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.model.defaultUserPreferences
import com.madassignment.myaccomodationapp.domain.model.toListingFilters
import com.madassignment.myaccomodationapp.domain.usecase.FetchListingsPageUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserProfileUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val saveUserPreferences: SaveUserPreferencesUseCase,
    private val fetchListingsPage: FetchListingsPageUseCase,
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

    val listings: Flow<PagingData<Listing>> =
        _appliedFilters
            .flatMapLatest { filters ->
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { ListingPagingSource(fetchListingsPage, filters) },
                ).flow
            }
            .cachedIn(viewModelScope)

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
