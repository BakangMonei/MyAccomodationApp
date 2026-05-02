package com.madassignment.myaccomodationapp.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.GaboroneRegion
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveProviderListingsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.UpsertListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val observeProviderListings: ObserveProviderListingsUseCase,
    private val upsertListing: UpsertListingUseCase,
) : ViewModel() {

    private val uidFlow = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myListings: StateFlow<List<Listing>> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeProviderListings(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveDraft(
        displayName: String,
        title: String,
        price: Double,
        deposit: Double,
        location: String,
        type: String,
        amenitiesText: String,
        imageUrlsText: String,
        availabilityMillis: Long,
        existingId: String?,
    ) {
        val uid = uidFlow.value ?: return
        val amenities = amenitiesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val splitUrls = imageUrlsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val urls = splitUrls.takeIf { it.isNotEmpty() }
            ?: listOf("https://via.placeholder.com/800x600.png?text=Accommodation")
        val listing = Listing(
            id = existingId.orEmpty(),
            title = title,
            price = price,
            depositAmount = deposit,
            location = location.ifBlank { GaboroneRegion.CBD },
            type = type.ifBlank { "Single Room" },
            amenities = amenities,
            availabilityDate = Instant.ofEpochMilli(availabilityMillis),
            imageUrls = urls,
            status = ListingStatus.Available,
            providerId = uid,
            providerDisplayName = displayName,
            createdAt = Instant.now(),
        )
        viewModelScope.launch { upsertListing(listing) }
    }
}
