package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

data class ListingFilters(
    val minPrice: Double,
    val maxPrice: Double,
    val locations: List<String>,
    val types: List<String>,
    val availabilityOnOrBefore: Instant?,
)

fun ListingFilters.matches(listing: Listing): Boolean {
    if (listing.price < minPrice || listing.price > maxPrice) return false
    if (locations.isNotEmpty() && !locations.contains(listing.location)) return false
    if (types.isNotEmpty() && !types.contains(listing.type)) return false
    availabilityOnOrBefore?.let { cutoff ->
        if (listing.availabilityDate.isAfter(cutoff)) return false
    }
    return true
}

fun UserPreferences.toListingFilters(): ListingFilters =
    ListingFilters(
        minPrice = minPriceBwp,
        maxPrice = maxPriceBwp,
        locations = locations,
        types = types,
        availabilityOnOrBefore = availabilityOnOrBefore,
    )
