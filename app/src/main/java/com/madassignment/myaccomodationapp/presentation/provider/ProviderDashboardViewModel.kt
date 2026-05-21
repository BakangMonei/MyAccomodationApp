package com.madassignment.myaccomodationapp.presentation.provider

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.data.repository.FirebaseListingImageUploader
import com.madassignment.myaccomodationapp.domain.model.GaboroneRegion
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.usecase.DeleteListingUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveProviderListingsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveProviderReservationsUseCase
import com.madassignment.myaccomodationapp.domain.usecase.UpdateListingUseCase
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
import kotlinx.coroutines.flow.flowOf
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
    private val observeProviderReservations: ObserveProviderReservationsUseCase,
    private val upsertListing: UpsertListingUseCase,
    private val updateListing: UpdateListingUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val imageUploader: FirebaseListingImageUploader,
) : ViewModel() {

    private val uidFlow = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val myListings: StateFlow<List<Listing>> = uidFlow
        .filterNotNull()
        .flatMapLatest { observeProviderListings(it) }
        .map { list -> list.sortedByDescending { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeReservations: StateFlow<List<Reservation>> = uidFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else observeProviderReservations(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reservationByListingId: StateFlow<Map<String, Reservation>> = activeReservations
        .map { reservations -> reservations.associateBy { it.listingId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _editingListing = MutableStateFlow<Listing?>(null)
    val editingListing: StateFlow<Listing?> = _editingListing.asStateFlow()

    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun loadForEdit(listing: Listing) {
        _editingListing.value = listing
    }

    fun clearEditor() {
        _editingListing.value = null
    }

    fun saveListing(
        displayName: String,
        title: String,
        price: Double,
        deposit: Double,
        location: String,
        type: String,
        amenitiesText: String,
        imageUris: List<Uri>,
        availabilityMillis: Long,
    ) {
        val uid = uidFlow.value ?: return
        val existing = _editingListing.value
        if (existing == null && imageUris.isEmpty()) {
            _events.tryEmit("Add at least one photo from your gallery.")
            return
        }
        if (existing != null && imageUris.isEmpty() && existing.imageUrls.isEmpty()) {
            _events.tryEmit("Listing must have at least one photo.")
            return
        }
        val amenities = amenitiesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        viewModelScope.launch {
            _publishing.value = true
            val imageUrls = if (imageUris.isNotEmpty()) {
                imageUploader.uploadListingImages(uid, imageUris).fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        _publishing.value = false
                        _events.emit(e.message ?: "Could not upload images")
                        return@launch
                    },
                )
            } else {
                existing!!.imageUrls
            }
            val result = if (existing == null) {
                val listing = Listing(
                    id = "",
                    title = title,
                    price = price,
                    depositAmount = deposit,
                    location = location.ifBlank { GaboroneRegion.CBD },
                    type = type.ifBlank { "Single Room" },
                    amenities = amenities,
                    availabilityDate = Instant.ofEpochMilli(availabilityMillis),
                    imageUrls = imageUrls,
                    status = ListingStatus.Available,
                    providerId = uid,
                    providerDisplayName = displayName,
                    createdAt = Instant.now(),
                )
                upsertListing(listing).map { }
            } else {
                val updated = existing.copy(
                    title = title,
                    price = price,
                    depositAmount = deposit,
                    location = location.ifBlank { GaboroneRegion.CBD },
                    type = type.ifBlank { "Single Room" },
                    amenities = amenities,
                    availabilityDate = Instant.ofEpochMilli(availabilityMillis),
                    imageUrls = imageUrls,
                    providerDisplayName = displayName,
                )
                updateListing(updated)
            }
            _publishing.value = false
            result.fold(
                onSuccess = {
                    if (existing == null) {
                        _events.emit("Listing published")
                    } else {
                        _events.emit("Listing updated")
                        clearEditor()
                    }
                },
                onFailure = { e ->
                    _events.emit(e.message ?: "Could not save listing")
                },
            )
        }
    }

    fun deleteListing(listingId: String) {
        val uid = uidFlow.value ?: return
        if (reservationByListingId.value.containsKey(listingId)) {
            _events.tryEmit(
                "Cannot delete while a student has an active reservation. Ask them to cancel first.",
            )
            return
        }
        viewModelScope.launch {
            _publishing.value = true
            deleteListingUseCase(listingId, uid).fold(
                onSuccess = {
                    _publishing.value = false
                    if (_editingListing.value?.id == listingId) {
                        clearEditor()
                    }
                    _events.emit("Listing deleted")
                },
                onFailure = { e ->
                    _publishing.value = false
                    _events.emit(e.message ?: "Could not delete listing")
                },
            )
        }
    }
}
