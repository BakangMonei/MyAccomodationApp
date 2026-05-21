package com.madassignment.myaccomodationapp.domain.usecase

import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.model.ListingPage
import com.madassignment.myaccomodationapp.domain.repository.ListingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchListingsPageUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    suspend operator fun invoke(
        filters: ListingFilters,
        pageSize: Int,
        cursor: String?,
    ): Result<ListingPage> = listingRepository.fetchPage(filters, pageSize, cursor)
}

class ObserveFilteredListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    operator fun invoke(filters: ListingFilters): Flow<List<Listing>> =
        listingRepository.observeFilteredAvailableListings(filters)
}

class ObserveListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    operator fun invoke(listingId: String): Flow<Listing?> = listingRepository.observeListing(listingId)
}

class UpsertListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    suspend operator fun invoke(listing: Listing): Result<Unit> = listingRepository.upsertListing(listing)
}

class UpdateListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    suspend operator fun invoke(listing: Listing): Result<Unit> = listingRepository.updateListing(listing)
}

class DeleteListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    suspend operator fun invoke(listingId: String, providerId: String): Result<Unit> =
        listingRepository.deleteListing(listingId, providerId)
}

class ObserveProviderListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository,
) {
    operator fun invoke(providerId: String): Flow<List<Listing>> =
        listingRepository.observeListingsForProvider(providerId)
}
