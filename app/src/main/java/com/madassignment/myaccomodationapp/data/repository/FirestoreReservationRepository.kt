package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.data.mapper.toReservationOrNull
import com.madassignment.myaccomodationapp.data.util.ReceiptNumbers
import com.madassignment.myaccomodationapp.domain.exception.BookingConflictException
import com.madassignment.myaccomodationapp.domain.model.ChatIds
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

            val providerId = listingSnap.getString("providerId")
            val listingTitle = listingSnap.getString("title").orEmpty()
            if (!providerId.isNullOrBlank()) {
                val chatId = ChatIds.forStudentAndProvider(userId, providerId)
                val chatRef = firestore.collection("chats").document(chatId)
                val depositText =
                    "I paid the deposit (P${depositAmount.toInt()}) for \"$listingTitle\". Receipt: $receipt"
                transaction.set(
                    chatRef,
                    mapOf(
                        "chatId" to chatId,
                        "participantIds" to listOf(userId, providerId).sorted(),
                        "lastMessageText" to depositText,
                        "lastSenderId" to userId,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastActivityAt" to FieldValue.serverTimestamp(),
                        "unread" to mapOf(providerId to 1),
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
                val messageRef = chatRef.collection("messages").document()
                transaction.set(
                    messageRef,
                    mapOf(
                        "chatId" to chatId,
                        "senderId" to userId,
                        "text" to depositText,
                        "sentAt" to FieldValue.serverTimestamp(),
                        "readBy" to listOf(userId),
                    ),
                )
            }

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
