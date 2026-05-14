package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.domain.model.AuthUser
import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.domain.model.defaultUserPreferences
import com.madassignment.myaccomodationapp.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val currentUser = auth.currentUser
        trySend(currentUser?.let { AuthUser(uid = it.uid, email = it.email) })

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(
                user?.let { AuthUser(uid = it.uid, email = it.email) },
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val credential = auth.signInWithEmailAndPassword(email, password).await()
        val uid = credential.user?.uid ?: return@runCatching
        val userRef = firestore.collection("users").document(uid)
        val snap = userRef.get().await()
        if (!snap.exists()) {
            userRef.set(
                mapOf(
                    "role" to UserRole.Student.name,
                    "displayName" to email.substringBefore("@"),
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "preferences" to preferenceSeedMap(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
    ): Result<Unit> = runCatching {
        val credential = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = credential.user?.uid ?: error("Missing UID after signup")
        val userDoc = mapOf(
            "role" to role.name,
            "displayName" to displayName,
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp(),
            "preferences" to preferenceSeedMap(),
        )
        firestore.collection("users").document(uid).set(userDoc, SetOptions.merge()).await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteAccount(reAuthPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val email = user.email ?: error("Email required for reauthentication")
        val credential = EmailAuthProvider.getCredential(email, reAuthPassword)
        user.reauthenticate(credential).await()
        firestore.collection("users").document(user.uid).delete().await()
        user.delete().await()
    }

    private fun preferenceSeedMap(): Map<String, Any?> {
        val p = defaultUserPreferences()
        return mapOf(
            "minPriceBwp" to p.minPriceBwp,
            "maxPriceBwp" to p.maxPriceBwp,
            "locations" to p.locations,
            "types" to p.types,
            "availabilityOnOrBefore" to null,
            "campusLatitude" to null,
            "campusLongitude" to null,
            "fcmToken" to null,
        )
    }
}
