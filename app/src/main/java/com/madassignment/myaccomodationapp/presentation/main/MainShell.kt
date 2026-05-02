package com.madassignment.myaccomodationapp.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.presentation.chat.ChatsPlaceholder
import com.madassignment.myaccomodationapp.presentation.home.HomeRoute
import com.madassignment.myaccomodationapp.presentation.navigation.Routes
import com.madassignment.myaccomodationapp.presentation.profile.ProfileRoute
import com.madassignment.myaccomodationapp.presentation.provider.ProviderDashboardRoute

@Composable
fun MainShell(
    rootNav: NavHostController,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val profile by mainViewModel.profile.collectAsStateWithLifecycle()
    val unread by mainViewModel.unreadChats.collectAsStateWithLifecycle()
    val isProvider = profile?.role == UserRole.Provider

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MailOutline, contentDescription = null)
                            if (unread > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    unread.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    label = { Text("Chats") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") },
                )
                if (isProvider) {
                    NavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3 },
                        icon = { Icon(Icons.Default.Store, contentDescription = null) },
                        label = { Text("Provider") },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeRoute(
                    onOpenListing = { id -> rootNav.navigate(Routes.listingDetail(id)) },
                    modifier = Modifier.fillMaxSize(),
                )
                1 -> ChatsPlaceholder(Modifier.fillMaxSize())
                2 -> ProfileRoute(Modifier.fillMaxSize())
                3 -> if (isProvider) {
                    ProviderDashboardRoute(Modifier.fillMaxSize())
                } else {
                    ProfileRoute(Modifier.fillMaxSize())
                }
                else -> HomeRoute(
                    onOpenListing = { id -> rootNav.navigate(Routes.listingDetail(id)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
