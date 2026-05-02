package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

enum class ListingStatus(val wireValue: String) {
    Available("Available"),
    Reserved("Reserved"),
}

data class Listing(
    val id: String,
    val title: String,
    val price: Double,
    val depositAmount: Double,
    val location: String,
    val type: String,
    val amenities: List<String>,
    val availabilityDate: Instant,
    val imageUrls: List<String>,
    val status: ListingStatus,
    val providerId: String,
    val providerDisplayName: String,
    val createdAt: Instant,
    val reservedBy: String? = null,
    val reservedAt: Instant? = null,
)
