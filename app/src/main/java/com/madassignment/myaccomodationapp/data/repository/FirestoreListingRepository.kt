package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.data.mapper.toFirestoreMap
import com.madassignment.myaccomodationapp.data.mapper.toListingOrNull
import com.madassignment.myaccomodationapp.data.util.FirestoreCursors
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
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
        // INDEX REQUIRED: listings — status == + price range + orderBy price ASC, createdAt DESC
        // INDEX REQUIRED: listings — status + price range + location (equality OR whereIn) + orderBy price + createdAt
        // INDEX REQUIRED: listings — status + price range + location + type (client-filter type when both multi-select due Firestore whereIn limits)
        var query: Query = firestore.collection(COLLECTION)
            .whereEqualTo("status", ListingStatus.Available.wireValue)
            .whereGreaterThanOrEqual("price", filters.minPrice)
            .whereLessThanOrEqual("price", filters.maxPrice)

        when (filters.locations.size) {
            1 -> {
                query = query.whereEqualTo("location", filters.locations.first())
            }
            in 2..10 -> {
                query = query.whereIn("location", filters.locations)
            }
        }

        query = query
            .orderBy("price", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)

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
        if (listing.id.isBlank()) {
            val doc = firestore.collection(COLLECTION).document()
            doc.set(listing.copy(id = doc.id).toFirestoreMap()).await()
        } else {
            firestore.collection(COLLECTION).document(listing.id)
                .set(listing.toFirestoreMap())
                .await()
        }
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
