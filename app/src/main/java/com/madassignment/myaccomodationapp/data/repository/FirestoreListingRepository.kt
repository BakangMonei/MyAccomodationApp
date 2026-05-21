package com.madassignment.myaccomodationapp.data.repository

import android.util.Log
import com.madassignment.myaccomodationapp.data.mapper.toFirestoreMap
import com.madassignment.myaccomodationapp.data.mapper.toListingOrNull
import com.madassignment.myaccomodationapp.data.mapper.toProviderEditableMap
import com.madassignment.myaccomodationapp.data.util.FirestoreCursors
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.model.matches
import com.madassignment.myaccomodationapp.domain.model.ListingPage
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.repository.ListingRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreListingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ListingRepository {

    override suspend fun fetchPage(
        filters: ListingFilters,
        pageSize: Int,
        cursor: String?,
    ): Result<ListingPage> = runCatching {
        var query = buildBaseQuery(filters)

        if (cursor != null) {
            val path = FirestoreCursors.decodePath(cursor)
            val snapshot = firestore.document(path).get().await()
            query = query.startAfter(snapshot)
        }

        val batchLimit = (pageSize * 3).coerceIn(30, 90).toLong()
        val snapshot = query.limit(batchLimit).get().await()

        val matched = snapshot.documents.asSequence()
            .mapNotNull { it.toListingOrNull() }
            .filter { filters.matches(it) }
            .take(pageSize)
            .toList()

        val lastDoc: DocumentSnapshot? = snapshot.documents.lastOrNull()
        val nextCursor = if (lastDoc != null && snapshot.size() >= batchLimit) {
            FirestoreCursors.encodePath(lastDoc.reference.path)
        } else {
            null
        }

        ListingPage(items = matched, nextCursor = nextCursor)
    }

    override fun observeFilteredAvailableListings(
        filters: ListingFilters,
        limit: Long,
    ): Flow<List<Listing>> = callbackFlow {
        // We use a query that is guaranteed to work without custom composite indexes
        // price filtering and secondary sorting are handled client-side for maximum reliability
        // Single-field whereIn works without a composite index; client-side filter + sort as before.
        val query = firestore.collection(COLLECTION)
            .whereIn(
                "status",
                listOf(ListingStatus.Available.wireValue, ListingStatus.Reserved.wireValue),
            )
            .limit(limit)

        val reg = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreListing", "Error fetching listings: ${error.message}", error)
                // We do NOT send emptyList() here. If the listener fails, we just don't update.
                // This prevents listings from "disappearing" on transient errors.
                return@addSnapshotListener
            }
            
            val items = snapshot?.documents.orEmpty()
                .asSequence()
                .mapNotNull { it.toListingOrNull() }
                .filter { filters.matches(it) }
                .sortedWith(compareBy<Listing> { it.price }.thenByDescending { it.createdAt })
                .toList()
            
            trySend(items)
        }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    private fun buildBaseQuery(filters: ListingFilters): Query {
        // Simple base query for pagination support
        return firestore.collection(COLLECTION)
            .whereIn(
                "status",
                listOf(ListingStatus.Available.wireValue, ListingStatus.Reserved.wireValue),
            )
    }

    override fun observeListing(listingId: String): Flow<Listing?> = callbackFlow {
        val reg = firestore.collection(COLLECTION).document(listingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toListingOrNull())
            }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override suspend fun createListing(listing: Listing): Result<String> = runCatching {
        val doc = firestore.collection(COLLECTION).document()
        val withId = if (listing.id.isBlank()) listing.copy(id = doc.id) else listing
        doc.set(withId.toFirestoreMap()).await()
        doc.id
    }

    override suspend fun upsertListing(listing: Listing): Result<Unit> = runCatching {
        val doc = firestore.collection(COLLECTION).document()
        doc.set(listing.copy(id = doc.id).toFirestoreMap()).await()
    }

    override suspend fun updateListing(listing: Listing): Result<Unit> = runCatching {
        require(listing.id.isNotBlank()) { "Listing id required for update" }
        val ref = firestore.collection(COLLECTION).document(listing.id)
        val snap = ref.get().await()
        if (!snap.exists()) {
            throw IllegalStateException("Listing not found")
        }
        if (snap.getString("providerId") != listing.providerId) {
            throw IllegalStateException("Not your listing")
        }
        ref.update(listing.toProviderEditableMap()).await()
    }

    override suspend fun deleteListing(listingId: String, providerId: String): Result<Unit> = runCatching {
        val ref = firestore.collection(COLLECTION).document(listingId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            throw IllegalStateException("Listing not found")
        }
        if (snap.getString("providerId") != providerId) {
            throw IllegalStateException("Not your listing")
        }
        ref.delete().await()
    }

    override fun observeListingsForProvider(providerId: String): Flow<List<Listing>> = callbackFlow {
        val reg = firestore.collection(COLLECTION)
            .whereEqualTo("providerId", providerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { it.toListingOrNull() }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    private companion object {
        const val COLLECTION = "listings"
    }
}
