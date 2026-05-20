package com.madassignment.myaccomodationapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.madassignment.myaccomodationapp.notifications.NotificationHelper
import com.madassignment.myaccomodationapp.presentation.navigation.AccommodationNavHost
import com.madassignment.myaccomodationapp.ui.theme.AccommodationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingListingId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingListingId = intent.listingIdExtra()
        enableEdgeToEdge()
        setContent {
            AccommodationTheme {
                Surface(Modifier.fillMaxSize()) {
                    AccommodationNavHost(
                        pendingListingId = pendingListingId,
                        onPendingListingConsumed = { pendingListingId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.listingIdExtra()?.let { pendingListingId = it }
    }

    private fun Intent.listingIdExtra(): String? =
        getStringExtra(NotificationHelper.EXTRA_LISTING_ID)
}
