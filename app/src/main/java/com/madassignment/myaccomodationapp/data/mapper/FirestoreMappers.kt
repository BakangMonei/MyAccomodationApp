package com.madassignment.myaccomodationapp.data.mapper

import com.madassignment.myaccomodationapp.domain.model.ChatMessage
import com.madassignment.myaccomodationapp.domain.model.ChatThread
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.model.ReservationStatus
import com.madassignment.myaccomodationapp.domain.model.UserPreferences
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.domain.model.defaultUserPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant

fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

fun DocumentSnapshot.toUserProfileOrNull(): UserProfile? {
    val uid = id
    val email = getString("email").orEmpty()
    val displayName = getString("displayName") ?: ""
    val roleWire = getString("role")
    val role = roleWire?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.Student
    val createdAt = getTimestamp("createdAt")?.toInstant() ?: Instant.now()
    val prefsMap = get("preferences") as? Map<*, *> ?: emptyMap<String, Any?>()
    val preferences = prefsMap.toUserPreferences()
    return UserProfile(
        uid = uid,
        email = email,
        displayName = displayName,
        role = role,
        createdAt = createdAt,
        preferences = preferences,
    )
}

@Suppress("UNCHECKED_CAST")
private fun Map<*, *>.toUserPreferences(): UserPreferences {
    val min = (this["minPriceBwp"] as? Number)?.toDouble() ?: defaultUserPreferences().minPriceBwp
    val max = (this["maxPriceBwp"] as? Number)?.toDouble() ?: defaultUserPreferences().maxPriceBwp
    val locs = (this["locations"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val types = (this["types"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val avail = this["availabilityOnOrBefore"] as? Timestamp
    val lat = (this["campusLatitude"] as? Number)?.toDouble()
    val lon = (this["campusLongitude"] as? Number)?.toDouble()
    val token = this["fcmToken"]?.toString()
    return UserPreferences(
        minPriceBwp = min,
        maxPriceBwp = max,
        locations = locs,
        types = types,
        availabilityOnOrBefore = avail?.toInstant(),
        campusLatitude = lat,
        campusLongitude = lon,
        fcmToken = token,
    )
}

fun UserPreferences.toFirestoreMap(): Map<String, Any?> = mapOf(
    "minPriceBwp" to minPriceBwp,
    "maxPriceBwp" to maxPriceBwp,
    "locations" to locations,
    "types" to types,
    "availabilityOnOrBefore" to availabilityOnOrBefore?.toTimestamp(),
    "campusLatitude" to campusLatitude,
    "campusLongitude" to campusLongitude,
    "fcmToken" to fcmToken,
)

fun DocumentSnapshot.toListingOrNull(): Listing? {
    val id = id
    val title = getString("title") ?: return null
    val price = (get("price") as? Number)?.toDouble() ?: return null
    val deposit = (get("depositAmount") as? Number)?.toDouble() ?: return null
    val location = getString("location") ?: return null
    val type = getString("type") ?: return null
    val amenities = (get("amenities") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val availTs = getTimestamp("availabilityDate") ?: return null
    val images = (get("imageUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    if (images.isEmpty()) return null
    val statusWire = getString("status") ?: return null
    val status = ListingStatus.entries.firstOrNull { it.wireValue == statusWire } ?: return null
    val providerId = getString("providerId") ?: return null
    val providerName = getString("providerDisplayName") ?: return null
    val createdTs = getTimestamp("createdAt") ?: return null
    val reservedBy = getString("reservedBy")
    val reservedAt = getTimestamp("reservedAt")?.toInstant()
    return Listing(
        id = id,
        title = title,
        price = price,
        depositAmount = deposit,
        location = location,
        type = type,
        amenities = amenities,
        availabilityDate = availTs.toInstant(),
        imageUrls = images,
        status = status,
        providerId = providerId,
        providerDisplayName = providerName,
        createdAt = createdTs.toInstant(),
        reservedBy = reservedBy,
        reservedAt = reservedAt,
    )
}

fun Listing.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "title" to title,
        "price" to price,
        "depositAmount" to depositAmount,
        "location" to location,
        "type" to type,
        "amenities" to amenities,
        "availabilityDate" to availabilityDate.toTimestamp(),
        "imageUrls" to imageUrls,
        "status" to status.wireValue,
        "providerId" to providerId,
        "providerDisplayName" to providerDisplayName,
        "createdAt" to createdAt.toTimestamp(),
        "reservedBy" to reservedBy,
        "reservedAt" to reservedAt?.toTimestamp(),
    )

/** Fields a provider may change when editing; excludes status, reservation, and ownership. */
fun Listing.toProviderEditableMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "price" to price,
    "depositAmount" to depositAmount,
    "location" to location,
    "type" to type,
    "amenities" to amenities,
    "availabilityDate" to availabilityDate.toTimestamp(),
    "imageUrls" to imageUrls,
    "providerDisplayName" to providerDisplayName,
)

fun DocumentSnapshot.toReservationOrNull(): Reservation? {
    val id = id
    val listingId = getString("listingId") ?: return null
    val userId = getString("userId") ?: return null
    val amount = (get("amount") as? Number)?.toDouble() ?: return null
    val receipt = getString("receiptNumber") ?: return null
    val ts = getTimestamp("timestamp") ?: return null
    val providerId = getString("providerId")
    val payerEmail = getString("payerEmail")
    val listingTitle = getString("listingTitle").orEmpty()
    val depositAmount = (get("depositAmount") as? Number)?.toDouble() ?: amount
    val balanceAmount = (get("balanceAmount") as? Number)?.toDouble() ?: 0.0
    val balanceReceipt = getString("balanceReceiptNumber")
    val balancePaidAt = getTimestamp("balancePaidAt")?.toInstant()
    val statusWire = getString("status")
    val status = statusWire?.let { wire ->
        ReservationStatus.entries.firstOrNull { it.wireValue == wire }
    } ?: ReservationStatus.Active
    val studentDisplayName = getString("studentDisplayName")
    val cancelledAt = getTimestamp("cancelledAt")?.toInstant()
    return Reservation(
        id = id,
        listingId = listingId,
        userId = userId,
        amount = amount,
        receiptNumber = receipt,
        timestamp = ts.toInstant(),
        providerId = providerId,
        payerEmail = payerEmail,
        studentDisplayName = studentDisplayName,
        listingTitle = listingTitle,
        depositAmount = depositAmount,
        balanceAmount = balanceAmount,
        balanceReceiptNumber = balanceReceipt,
        balancePaidAt = balancePaidAt,
        status = status,
        cancelledAt = cancelledAt,
    )
}

fun DocumentSnapshot.toChatMessageOrNull(): ChatMessage? {
    val id = id
    val chatId = getString("chatId") ?: return null
    val sender = getString("senderId") ?: return null
    val text = getString("text") ?: return null
    val sent = getTimestamp("sentAt") ?: return null
    val readBy = (get("readBy") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    return ChatMessage(id, chatId, sender, text, sent.toInstant(), readBy)
}

fun DocumentSnapshot.toChatThreadOrNull(userId: String): ChatThread? {
    val chatId = getString("chatId") ?: id
    val participantIds =
        (get("participantIds") as? List<*>)?.mapNotNull { it?.toString() } ?: return null
    if (!participantIds.contains(userId)) return null
    val lastMessageAt =
        getTimestamp("lastMessageAt")?.toInstant()
            ?: getTimestamp("lastActivityAt")?.toInstant()
            ?: Instant.now()
    val lastMessageText = getString("lastMessageText").orEmpty()
    val lastSenderId = getString("lastSenderId")
    val unreadMap = get("unread") as? Map<*, *> ?: emptyMap<String, Any>()
    val unreadCount = (unreadMap[userId] as? Number)?.toInt() ?: 0
    val participantEmailsRaw = get("participantEmails") as? Map<*, *> ?: emptyMap<String, Any>()
    val participantEmails = participantEmailsRaw
        .mapNotNull { (key, value) ->
            val k = key?.toString() ?: return@mapNotNull null
            val v = value?.toString() ?: return@mapNotNull null
            k to v
        }
        .toMap()
    return ChatThread(
        chatId = chatId,
        participantIds = participantIds,
        participantEmails = participantEmails,
        lastMessageText = lastMessageText,
        lastSenderId = lastSenderId,
        lastMessageAt = lastMessageAt,
        unreadCount = unreadCount,
    )
}
