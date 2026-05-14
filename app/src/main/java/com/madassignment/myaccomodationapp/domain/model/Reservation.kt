package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

data class Reservation(
    val id: String,
    val listingId: String,
    val userId: String,
    val amount: Double,
    val receiptNumber: String,
    val timestamp: Instant,
    val providerId: String? = null,
    val payerEmail: String? = null,
)
