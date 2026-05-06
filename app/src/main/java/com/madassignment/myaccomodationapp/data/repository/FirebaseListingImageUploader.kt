package com.madassignment.myaccomodationapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseListingImageUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
) {
    suspend fun uploadListingImages(providerUid: String, uris: List<Uri>): Result<List<String>> = runCatching {
        require(uris.isNotEmpty()) { "Select at least one image" }
        val root = storage.reference.child("listings").child(providerUid)
        uris.mapIndexed { index, uri ->
            val ref = root.child("${UUID.randomUUID()}_${index}.jpg")
            context.contentResolver.openInputStream(uri).use { stream ->
                requireNotNull(stream) { "Could not read selected image" }
                ref.putStream(stream).await()
            }
            ref.getDownloadUrl().await().toString()
        }
    }
}
