package com.madassignment.myaccomodationapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.madassignment.myaccomodationapp.data.mapper.toInstant
import com.madassignment.myaccomodationapp.data.mapper.toReservationOrNull
import com.madassignment.myaccomodationapp.data.util.ReceiptNumbers
import com.madassignment.myaccomodationapp.domain.exception.BookingConflictException
import com.madassignment.myaccomodationapp.domain.model.ChatIds
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.model.ReservationStatus
import com.madassignment.myaccomodationapp.domain.repository.ReservationRepository
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
        val reservation = firestore.runTransaction { transaction ->
            val listingRef = firestore.collection("listings").document(listingId)
            val listingSnap = transaction.get(listingRef)
            if (!listingSnap.exists()) {
                throw IllegalStateException("Listing not found")
            }
            val status = listingSnap.getString("status")
            if (status != ListingStatus.Available.wireValue) {
                throw BookingConflictException()
            }
            val reservationRef = firestore.collection("reservations").document()
            val receipt = ReceiptNumbers.next(Instant.now())
            val userRef = firestore.collection("users").document(userId)
            val userSnap = transaction.get(userRef)
            val payerEmail = userSnap.getString("email")
            val studentDisplayName = userSnap.getString("displayName")
            val listingTitle = listingSnap.getString("title").orEmpty()
            val monthlyPrice = (listingSnap.get("price") as? Number)?.toDouble() ?: 0.0
            val deposit = depositAmount.coerceAtLeast(0.0)
            val balanceDue = (monthlyPrice - deposit).coerceAtLeast(0.0)
            val providerId = listingSnap.getString("providerId")

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
                    "amount" to deposit,
                    "receiptNumber" to receipt,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "providerId" to providerId,
                    "payerEmail" to payerEmail,
                    "studentDisplayName" to studentDisplayName,
                    "listingTitle" to listingTitle,
                    "depositAmount" to deposit,
                    "balanceAmount" to balanceDue,
                    "status" to ReservationStatus.Active.wireValue,
                ),
            )

            Reservation(
                id = reservationRef.id,
                listingId = listingId,
                userId = userId,
                amount = deposit,
                receiptNumber = receipt,
                timestamp = Instant.now(),
                providerId = providerId,
                payerEmail = payerEmail,
                studentDisplayName = studentDisplayName,
                listingTitle = listingTitle,
                depositAmount = deposit,
                balanceAmount = balanceDue,
                balanceReceiptNumber = null,
                balancePaidAt = null,
                status = ReservationStatus.Active,
            )
        }.await()

        notifyProviderPayment(
            userId = userId,
            providerId = reservation.providerId,
            payerEmail = reservation.payerEmail,
            listingTitle = reservation.listingTitle,
            amountPaid = reservation.depositAmount,
            receipt = reservation.receiptNumber,
            isBalance = false,
            depositReceipt = null,
        )

        reservation
    }

    override suspend fun payReservationBalance(
        reservationId: String,
        userId: String,
    ): Result<Reservation> = runCatching {
        val reservation = firestore.runTransaction { transaction ->
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
            val studentDisplayName = resSnap.getString("studentDisplayName")
            val listingTitle = resSnap.getString("listingTitle").orEmpty()
            val providerId = resSnap.getString("providerId")
            val depositReceipt = resSnap.getString("receiptNumber")
                ?: throw IllegalStateException("Missing receipt")
            val createdInstant = resSnap.getTimestamp("timestamp")?.toInstant() ?: Instant.now()
            val statusWire = resSnap.getString("status")
            val status = statusWire?.let { wire ->
                ReservationStatus.entries.firstOrNull { it.wireValue == wire }
            } ?: ReservationStatus.Active

            Reservation(
                id = reservationId,
                listingId = listingId,
                userId = userId,
                amount = newTotalPaid,
                receiptNumber = depositReceipt,
                timestamp = createdInstant,
                providerId = providerId,
                payerEmail = payerEmail,
                studentDisplayName = studentDisplayName,
                listingTitle = listingTitle,
                depositAmount = depositAmount,
                balanceAmount = balanceAmountStored,
                balanceReceiptNumber = balanceReceipt,
                balancePaidAt = Instant.now(),
                status = status,
            )
        }.await()

        notifyProviderPayment(
            userId = userId,
            providerId = reservation.providerId,
            payerEmail = reservation.payerEmail,
            listingTitle = reservation.listingTitle,
            amountPaid = reservation.balanceAmount,
            receipt = reservation.balanceReceiptNumber.orEmpty(),
            isBalance = true,
            depositReceipt = reservation.receiptNumber,
        )

        reservation
    }

    override suspend fun cancelReservation(reservationId: String, userId: String): Result<Unit> = runCatching {
        val cancelMeta = firestore.runTransaction { transaction ->
            val reservationRef = firestore.collection("reservations").document(reservationId)
            val resSnap = transaction.get(reservationRef)
            if (!resSnap.exists()) {
                throw IllegalStateException("Reservation not found")
            }
            if (resSnap.getString("userId") != userId) {
                throw IllegalStateException("Not your reservation")
            }
            val listingTitle = resSnap.getString("listingTitle").orEmpty()
            val providerId = resSnap.getString("providerId")
            val currentStatus = resSnap.getString("status") ?: ReservationStatus.Active.wireValue
            if (currentStatus == ReservationStatus.Cancelled.wireValue) {
                return@runTransaction listingTitle to providerId
            }

            val listingId = resSnap.getString("listingId")
                ?: throw IllegalStateException("Missing listing")
            val listingRef = firestore.collection("listings").document(listingId)
            val listingSnap = transaction.get(listingRef)
            if (listingSnap.exists() &&
                listingSnap.getString("status") == ListingStatus.Reserved.wireValue &&
                listingSnap.getString("reservedBy") == userId
            ) {
                transaction.update(
                    listingRef,
                    mapOf(
                        "status" to ListingStatus.Available.wireValue,
                        "reservedBy" to FieldValue.delete(),
                        "reservedAt" to FieldValue.delete(),
                    ),
                )
            }
            transaction.update(
                reservationRef,
                mapOf(
                    "status" to ReservationStatus.Cancelled.wireValue,
                    "cancelledAt" to FieldValue.serverTimestamp(),
                ),
            )
            listingTitle to providerId
        }.await()

        notifyProviderCancellation(
            userId = userId,
            providerId = cancelMeta.second,
            listingTitle = cancelMeta.first,
        )
    }

    private suspend fun notifyProviderCancellation(
        userId: String,
        providerId: String?,
        listingTitle: String,
    ) {
        if (providerId.isNullOrBlank() || providerId == userId) return
        runCatching {
            val userSnap = firestore.collection("users").document(userId).get().await()
            val studentLabel = userSnap.getString("displayName")
                ?: userSnap.getString("email")
                ?: userId
            val chatId = ChatIds.forStudentAndProvider(userId, providerId)
            val chatRef = firestore.collection("chats").document(chatId)
            val messageText = "I cancelled my reservation for \"$listingTitle\" (sandbox undo)."
            firestore.runBatch { batch ->
                batch.set(
                    chatRef,
                    mapOf(
                        "chatId" to chatId,
                        "participantIds" to listOf(userId, providerId).sorted(),
                        "lastMessageText" to messageText,
                        "lastSenderId" to userId,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastActivityAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                batch.set(
                    chatRef.collection("messages").document(),
                    mapOf(
                        "chatId" to chatId,
                        "senderId" to userId,
                        "text" to messageText,
                        "sentAt" to FieldValue.serverTimestamp(),
                        "readBy" to listOf(userId),
                    ),
                )
            }.await()
        }
    }

    /**
     * Best-effort landlord notification. Payment already succeeded; chat failures must not roll back booking.
     */
    private suspend fun notifyProviderPayment(
        userId: String,
        providerId: String?,
        payerEmail: String?,
        listingTitle: String,
        amountPaid: Double,
        receipt: String,
        isBalance: Boolean,
        depositReceipt: String?,
    ) {
        if (providerId.isNullOrBlank() || providerId == userId) return
        runCatching {
            val providerSnap = firestore.collection("users").document(providerId).get().await()
            val providerEmail = providerSnap.getString("email")
            val chatId = ChatIds.forStudentAndProvider(userId, providerId)
            val chatRef = firestore.collection("chats").document(chatId)
            val messageText = if (isBalance) {
                "I paid the balance (P${amountPaid.toInt()}) for \"$listingTitle\". Receipt: $receipt (deposit receipt: $depositReceipt)"
            } else {
                "I paid the deposit (P${amountPaid.toInt()}) for \"$listingTitle\". Receipt: $receipt"
            }
            firestore.runBatch { batch ->
                batch.set(
                    chatRef,
                    mapOf(
                        "chatId" to chatId,
                        "participantIds" to listOf(userId, providerId).sorted(),
                        "lastMessageText" to messageText,
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
                batch.set(
                    messageRef,
                    mapOf(
                        "chatId" to chatId,
                        "senderId" to userId,
                        "text" to messageText,
                        "sentAt" to FieldValue.serverTimestamp(),
                        "readBy" to listOf(userId),
                    ),
                )
                batch.update(
                    chatRef,
                    com.google.firebase.firestore.FieldPath.of("unread", providerId),
                    FieldValue.increment(1),
                )
            }.await()
        }
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
                val items = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toReservationOrNull() }
                    .filter { it.isActive }
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
                val items = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toReservationOrNull() }
                    .filter { it.isActive }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    private companion object {
        // INDEX: reservations (userId ASC, timestamp DESC), (providerId ASC, timestamp DESC)
    }
}
