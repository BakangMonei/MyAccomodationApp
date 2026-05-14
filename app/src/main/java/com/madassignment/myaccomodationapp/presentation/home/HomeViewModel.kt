package com.madassignment.myaccomodationapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.defaultUserPreferences
import com.madassignment.myaccomodationapp.domain.model.toListingFilters
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveFilteredListingsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveListingUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserProfileUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveUserReservationsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val saveUserPreferences: SaveUserPreferencesUseCase,
    private val observeFilteredListings: ObserveFilteredListingsUseCase,
    private val observeUserReservations: ObserveUserReservationsUseCase,
    private val observeListing: ObserveListingUseCase,
) : ViewModel() {

    private val authUid = observeAuthState()
        .map { it?.uid }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Signed-in user id for partitioning home list (guest = null). */
    val currentUserId: StateFlow<String?> get() = authUid

    val userProfile = authUid
        .filterNotNull()
        .flatMapLatest { uid -> observeUserProfile(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _appliedFilters = MutableStateFlow(
        ListingFilters(0.0, 30_000.0, emptyList(), emptyList(), null),
    )
    val appliedFilters: StateFlow<ListingFilters> = _appliedFilters.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        // Sync filters with user preferences when they load
        userProfile
            .filterNotNull()
            .onEach { profile ->
                val fromProfile = profile.preferences.toListingFilters()
                // Only update if different to avoid restarting the listings flow unnecessarily
                if (fromProfile != _appliedFilters.value) {
                    _appliedFilters.value = fromProfile
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * StateFlow holding the current listings. 
     * Starts as null (loading), then becomes a list (even if empty).
     */
    val listings: StateFlow<List<Listing>?> =
        _appliedFilters
            .flatMapLatest { filters ->
                observeFilteredListings(filters)
                    .map<List<Listing>, List<Listing>?> { it }
                    .catch { 
                        _events.emit("Connection error")
                        // We don't emit emptyList here to prevent the UI from "disappearing" data on error
                    }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val takenByMeListings: StateFlow<List<Listing>> = authUid
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                observeUserReservations(uid).flatMapLatest { reservations ->
                    val listingIds = reservations.map { it.listingId }.distinct()
                    if (listingIds.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(listingIds.map { id -> observeListing(id) }) { array ->
                            array.mapNotNull { it as? Listing }
                                .filter { listing ->
                                    listing.status == ListingStatus.Reserved && listing.reservedBy == uid
                                }
                                .sortedByDescending { it.reservedAt ?: it.createdAt }
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun applyFilters(filters: ListingFilters) {
        _appliedFilters.value = filters
        val uid = authUid.value ?: return
        viewModelScope.launch {
            try {
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
                ).getOrThrow()
                _events.emit("Preferences updated")
            } catch (e: Exception) {
                _events.emit("Applied locally")
            }
        }
    }
}
