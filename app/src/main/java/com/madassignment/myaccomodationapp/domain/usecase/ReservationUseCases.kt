package com.madassignment.myaccomodationapp.domain.usecase

import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReserveListingUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository,
) {
    suspend operator fun invoke(
        listingId: String,
        userId: String,
        depositAmount: Double,
    ): Result<Reservation> = reservationRepository.reserveListing(listingId, userId, depositAmount)
}

class ObserveUserReservationsUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository,
) {
    operator fun invoke(userId: String): Flow<List<Reservation>> =
        reservationRepository.observeReservationsForUser(userId)
}

class ObserveProviderReservationsUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository,
) {
    operator fun invoke(providerId: String): Flow<List<Reservation>> =
        reservationRepository.observeReservationsForProvider(providerId)
}
