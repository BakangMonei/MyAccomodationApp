package com.madassignment.myaccomodationapp.fcm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CampusMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(FieldPath.of("preferences", "fcmToken"), token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Extend with local notification display when targeting custom data payloads.
    }
}
