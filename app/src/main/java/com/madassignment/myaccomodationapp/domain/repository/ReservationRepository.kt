package com.madassignment.myaccomodationapp.domain.repository

import com.madassignment.myaccomodationapp.domain.model.Reservation
import kotlinx.coroutines.flow.Flow

interface ReservationRepository {
    suspend fun reserveListing(listingId: String, userId: String, depositAmount: Double): Result<Reservation>

    fun observeReservationsForUser(userId: String): Flow<List<Reservation>>

    fun observeReservationsForProvider(providerId: String): Flow<List<Reservation>>
}
