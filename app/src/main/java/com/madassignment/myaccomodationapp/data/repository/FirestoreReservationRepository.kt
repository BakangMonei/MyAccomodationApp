package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.data.mapper.toReservationOrNull
import com.madassignment.myaccomodationapp.data.util.ReceiptNumbers
import com.madassignment.myaccomodationapp.domain.exception.BookingConflictException
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.repository.ReservationRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreReservationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ReservationRepository {

    override suspend fun reserveListing(
        listingId: String,
        userId: String,
        depositAmount: Double,
    ): Result<Reservation> = runCatching {
        firestore.runTransaction { transaction ->
            val listingRef = firestore.collection("listings").document(listingId)
            val listingSnap = transaction.get(listingRef)
            val status = listingSnap.getString("status")
            if (status != ListingStatus.Available.wireValue) {
                throw BookingConflictException()
            }
            val reservationRef = firestore.collection("reservations").document()
            val receipt = ReceiptNumbers.next(Instant.now())
            transaction.update(
                listingRef,
                mapOf(
                    "status" to ListingStatus.Reserved.wireValue,
                    "reservedBy" to userId,
                    "reservedAt" to FieldValue.serverTimestamp(),
                ),
            )
            transaction.set(
                reservationRef,
                mapOf(
                    "listingId" to listingId,
                    "userId" to userId,
                    "amount" to depositAmount,
                    "receiptNumber" to receipt,
                    "timestamp" to FieldValue.serverTimestamp(),
                ),
            )
            Reservation(
                id = reservationRef.id,
                listingId = listingId,
                userId = userId,
                amount = depositAmount,
                receiptNumber = receipt,
                timestamp = Instant.now(),
            )
        }.await()
    }

    override fun observeReservationsForUser(userId: String): Flow<List<Reservation>> = callbackFlow {
        val reg = firestore.collection("reservations")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { it.toReservationOrNull() }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    private companion object {
        // INDEX REQUIRED: reservations (userId ASC, timestamp DESC)
    }
}
