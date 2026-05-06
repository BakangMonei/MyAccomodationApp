package com.madassignment.myaccomodationapp.domain.repository

import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.model.ListingPage
import kotlinx.coroutines.flow.Flow

interface ListingRepository {
    suspend fun fetchPage(filters: ListingFilters, pageSize: Int, cursor: String?): Result<ListingPage>

    /**
     * Realtime listener for available listings matching the Firestore query window; filters that cannot
     * be expressed in Firestore (types, availability) are applied client-side. Caps server reads with [limit].
     */
    fun observeFilteredAvailableListings(filters: ListingFilters, limit: Long = 150): Flow<List<Listing>>

    fun observeListing(listingId: String): Flow<Listing?>

    suspend fun createListing(listing: Listing): Result<String>

    suspend fun upsertListing(listing: Listing): Result<Unit>

    fun observeListingsForProvider(providerId: String): Flow<List<Listing>>
}
