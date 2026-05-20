package com.madassignment.myaccomodationapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.madassignment.myaccomodationapp.MainActivity
import com.madassignment.myaccomodationapp.R

object NotificationHelper {

    const val CHANNEL_LISTING_ALERTS = "listing_alerts"
    const val EXTRA_LISTING_ID = "listingId"
    private const val NOTIFICATION_ID_LISTING_MATCH = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_LISTING_ALERTS,
            context.getString(R.string.notification_channel_listing_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_listing_alerts_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showListingMatchNotification(
        context: Context,
        title: String,
        body: String,
        listingId: String?,
    ) {
        ensureChannels(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            listingId?.let { putExtra(EXTRA_LISTING_ID, it) }
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(
            context,
            listingId?.hashCode() ?: 0,
            launchIntent,
            pendingFlags,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LISTING_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID_LISTING_MATCH + (listingId?.hashCode() ?: 0), notification)
    }
}
