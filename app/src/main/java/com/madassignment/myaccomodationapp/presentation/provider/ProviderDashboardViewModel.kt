package com.madassignment.myaccomodationapp.presentation.provider

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.data.repository.FirebaseListingImageUploader
import com.madassignment.myaccomodationapp.domain.model.GaboroneRegion
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveProviderListingsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.UpsertListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val imageUploader: FirebaseListingImageUploader,
) : ViewModel() {

    private val uidFlow = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myListings: StateFlow<List<Listing>> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeProviderListings(it) }
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun publishListing(
        displayName: String,
        title: String,
        price: Double,
        deposit: Double,
        location: String,
        type: String,
        amenitiesText: String,
        imageUris: List<Uri>,
        availabilityMillis: Long,
        existingId: String?,
    ) {
        val uid = uidFlow.value ?: return
        if (imageUris.isEmpty()) {
            _events.tryEmit("Add at least one photo from your gallery.")
            return
        }
        val amenities = amenitiesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        viewModelScope.launch {
            _publishing.value = true
            val urls = imageUploader.uploadListingImages(uid, imageUris).fold(
                onSuccess = { it },
                onFailure = { e ->
                    _publishing.value = false
                    _events.tryEmit(e.message ?: "Could not upload images")
                    return@launch
                },
            )
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
            upsertListing(listing).fold(
                onSuccess = {
                    _publishing.value = false
                    _events.tryEmit("Listing published")
                },
                onFailure = { e ->
                    _publishing.value = false
                    _events.tryEmit(e.message ?: "Could not save listing")
                },
            )
        }
    }
}
