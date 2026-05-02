package com.madassignment.myaccomodationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.madassignment.myaccomodationapp.presentation.navigation.AccommodationNavHost
import com.madassignment.myaccomodationapp.ui.theme.AccommodationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccommodationTheme {
                Surface(Modifier.fillMaxSize()) {
                    AccommodationNavHost()
                }
            }
        }
    }
}
