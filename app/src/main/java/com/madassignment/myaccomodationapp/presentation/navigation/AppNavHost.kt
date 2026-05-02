package com.madassignment.myaccomodationapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madassignment.myaccomodationapp.presentation.auth.AuthRoute
import com.madassignment.myaccomodationapp.presentation.chat.ChatRoute
import com.madassignment.myaccomodationapp.presentation.detail.DetailRoute
import com.madassignment.myaccomodationapp.presentation.main.MainShell
import com.madassignment.myaccomodationapp.presentation.payment.PaymentRoute
import com.madassignment.myaccomodationapp.presentation.root.RootViewModel

@Composable
fun AccommodationNavHost() {
    val navController = rememberNavController()
    val rootViewModel: RootViewModel = hiltViewModel()
    val user by rootViewModel.authUser.collectAsStateWithLifecycle()

    LaunchedEffect(user) {
        if (user == null) {
            val route = navController.currentDestination?.route
            if (route != Routes.AUTH) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH,
    ) {
        composable(Routes.AUTH) {
            AuthRoute(
                onAuthenticated = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            if (user != null) {
                MainShell(rootNav = navController)
            }
        }
        composable(
            route = Routes.LISTING_DETAIL,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType }),
        ) {
            DetailRoute(
                authUid = user?.uid,
                onBack = { navController.popBackStack() },
                onReserve = { id -> navController.navigate(Routes.payment(id)) },
                onOpenChat = { chatId, peerId ->
                    navController.navigate(Routes.chat(chatId, peerId))
                },
            )
        }
        composable(
            route = Routes.PAYMENT,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType }),
        ) {
            PaymentRoute(
                onBack = { navController.popBackStack() },
                onReceiptDone = {
                    navController.popBackStack()
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("peerId") { type = NavType.StringType },
            ),
        ) {
            ChatRoute(onBack = { navController.popBackStack() })
        }
    }
}
