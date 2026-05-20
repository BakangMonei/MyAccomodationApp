package com.madassignment.myaccomodationapp.fcm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.madassignment.myaccomodationapp.notifications.NotificationHelper

class CampusMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(FieldPath.of("preferences", "fcmToken"), token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New listing match"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "A listing matches your saved preferences"
        val listingId = message.data["listingId"]
        NotificationHelper.showListingMatchNotification(
            context = applicationContext,
            title = title,
            body = body,
            listingId = listingId,
        )
    }
}
