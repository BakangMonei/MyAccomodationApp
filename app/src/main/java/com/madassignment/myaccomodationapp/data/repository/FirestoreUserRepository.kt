package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.data.mapper.toFirestoreMap
import com.madassignment.myaccomodationapp.data.mapper.toUserProfileOrNull
import com.madassignment.myaccomodationapp.domain.model.UserPreferences
import com.madassignment.myaccomodationapp.domain.model.UserProfile
import com.madassignment.myaccomodationapp.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override suspend fun getProfile(uid: String): Result<UserProfile> = runCatching {
        val snap = firestore.collection("users").document(uid).get().await()
        snap.toUserProfileOrNull() ?: error("Malformed user profile")
    }

    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUserProfileOrNull())
            }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override suspend fun updatePreferences(uid: String, preferences: UserPreferences): Result<Unit> =
        runCatching {
            firestore.collection("users").document(uid)
                .set(mapOf("preferences" to preferences.toFirestoreMap()), SetOptions.merge())
                .await()
        }
}
