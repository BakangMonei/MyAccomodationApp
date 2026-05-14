package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.data.mapper.toInstant
import com.madassignment.myaccomodationapp.data.mapper.toReservationOrNull
import com.madassignment.myaccomodationapp.data.util.ReceiptNumbers
import com.madassignment.myaccomodationapp.domain.exception.BookingConflictException
import com.madassignment.myaccomodationapp.domain.model.ChatIds
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.repository.ReservationRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
            val userRef = firestore.collection("users").document(userId)
            val userSnap = transaction.get(userRef)
            val payerEmail = userSnap.getString("email")
            val listingTitle = listingSnap.getString("title").orEmpty()
            val monthlyPrice = (listingSnap.get("price") as? Number)?.toDouble() ?: 0.0
            val balanceDue = (monthlyPrice - depositAmount).coerceAtLeast(0.0)
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
                    "providerId" to listingSnap.getString("providerId"),
                    "payerEmail" to payerEmail,
                    "listingTitle" to listingTitle,
                    "depositAmount" to depositAmount,
                    "balanceAmount" to balanceDue,
                ),
            )

            val providerId = listingSnap.getString("providerId")
            if (!providerId.isNullOrBlank()) {
                val providerRef = firestore.collection("users").document(providerId)
                val providerSnap = transaction.get(providerRef)
                val providerEmail = providerSnap.getString("email")
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
                        "participantEmails" to mapOf(
                            userId to (payerEmail ?: ""),
                            providerId to (providerEmail ?: ""),
                        ),
                    ),
                    SetOptions.merge(),
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
                transaction.update(
                    chatRef,
                    com.google.firebase.firestore.FieldPath.of("unread", providerId),
                    FieldValue.increment(1),
                )
            }

            Reservation(
                id = reservationRef.id,
                listingId = listingId,
                userId = userId,
                amount = depositAmount,
                receiptNumber = receipt,
                timestamp = Instant.now(),
                providerId = providerId,
                payerEmail = payerEmail,
                listingTitle = listingTitle,
                depositAmount = depositAmount,
                balanceAmount = balanceDue,
                balanceReceiptNumber = null,
                balancePaidAt = null,
            )
        }.await()
    }

    override suspend fun payReservationBalance(
        reservationId: String,
        userId: String,
    ): Result<Reservation> = runCatching {
        firestore.runTransaction { transaction ->
            val reservationRef = firestore.collection("reservations").document(reservationId)
            val resSnap = transaction.get(reservationRef)
            if (!resSnap.exists()) {
                throw IllegalStateException("Reservation not found")
            }
            if (resSnap.getString("userId") != userId) {
                throw IllegalStateException("Not your reservation")
            }
            if (resSnap.getString("balanceReceiptNumber") != null) {
                throw IllegalStateException("Balance already paid")
            }
            val listingId = resSnap.getString("listingId")
                ?: throw IllegalStateException("Missing listing")
            val balanceAmountStored = (resSnap.get("balanceAmount") as? Number)?.toDouble() ?: 0.0
            if (balanceAmountStored <= 0) {
                throw IllegalStateException("No balance due")
            }
            val listingRef = firestore.collection("listings").document(listingId)
            val listingSnap = transaction.get(listingRef)
            if (listingSnap.getString("status") != ListingStatus.Reserved.wireValue) {
                throw IllegalStateException("Listing is no longer reserved")
            }
            if (listingSnap.getString("reservedBy") != userId) {
                throw IllegalStateException("Listing reservation mismatch")
            }

            val balanceReceipt = ReceiptNumbers.next(Instant.now())
            val depositAmount = (resSnap.get("depositAmount") as? Number)?.toDouble()
                ?: (resSnap.get("amount") as? Number)?.toDouble() ?: 0.0
            val currentPaid = (resSnap.get("amount") as? Number)?.toDouble() ?: depositAmount
            val newTotalPaid = currentPaid + balanceAmountStored

            transaction.update(
                reservationRef,
                mapOf(
                    "amount" to newTotalPaid,
                    "balanceReceiptNumber" to balanceReceipt,
                    "balancePaidAt" to FieldValue.serverTimestamp(),
                ),
            )

            val payerEmail = resSnap.getString("payerEmail")
            val listingTitle = resSnap.getString("listingTitle").orEmpty()
            val providerId = resSnap.getString("providerId")
            val depositReceipt = resSnap.getString("receiptNumber")
                ?: throw IllegalStateException("Missing receipt")
            if (!providerId.isNullOrBlank()) {
                val providerRef = firestore.collection("users").document(providerId)
                val providerSnap = transaction.get(providerRef)
                val providerEmail = providerSnap.getString("email")
                val chatId = ChatIds.forStudentAndProvider(userId, providerId)
                val chatRef = firestore.collection("chats").document(chatId)
                val balanceText =
                    "I paid the balance (P${balanceAmountStored.toInt()}) for \"$listingTitle\". Receipt: $balanceReceipt (deposit receipt: $depositReceipt)"
                transaction.set(
                    chatRef,
                    mapOf(
                        "chatId" to chatId,
                        "participantIds" to listOf(userId, providerId).sorted(),
                        "lastMessageText" to balanceText,
                        "lastSenderId" to userId,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastActivityAt" to FieldValue.serverTimestamp(),
                        "participantEmails" to mapOf(
                            userId to (payerEmail ?: ""),
                            providerId to (providerEmail ?: ""),
                        ),
                    ),
                    SetOptions.merge(),
                )
                val messageRef = chatRef.collection("messages").document()
                transaction.set(
                    messageRef,
                    mapOf(
                        "chatId" to chatId,
                        "senderId" to userId,
                        "text" to balanceText,
                        "sentAt" to FieldValue.serverTimestamp(),
                        "readBy" to listOf(userId),
                    ),
                )
                transaction.update(
                    chatRef,
                    com.google.firebase.firestore.FieldPath.of("unread", providerId),
                    FieldValue.increment(1),
                )
            }

            val createdInstant = resSnap.getTimestamp("timestamp")?.toInstant() ?: Instant.now()
            Reservation(
                id = reservationId,
                listingId = listingId,
                userId = userId,
                amount = newTotalPaid,
                receiptNumber = depositReceipt,
                timestamp = createdInstant,
                providerId = providerId,
                payerEmail = payerEmail,
                listingTitle = listingTitle,
                depositAmount = depositAmount,
                balanceAmount = balanceAmountStored,
                balanceReceiptNumber = balanceReceipt,
                balancePaidAt = Instant.now(),
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

    override fun observeReservationsForProvider(providerId: String): Flow<List<Reservation>> = callbackFlow {
        val reg = firestore.collection("reservations")
            .whereEqualTo("providerId", providerId)
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
