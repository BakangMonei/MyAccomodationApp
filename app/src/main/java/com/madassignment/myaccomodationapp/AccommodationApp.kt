package com.madassignment.myaccomodationapp

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.madassignment.myaccomodationapp.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AccommodationApp : Application() {
    override fun onCreate() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
