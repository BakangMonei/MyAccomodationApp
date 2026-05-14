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
        // We allow null user (guest) on MAIN, but certain actions will require auth
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
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
            MainShell(rootNav = navController)
        }
        composable(
            route = Routes.LISTING_DETAIL,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType }),
        ) {
            DetailRoute(
                authUid = user?.uid,
                onBack = { navController.popBackStack() },
                onReserve = { id ->
                    if (user != null) {
                        navController.navigate(Routes.payment(id))
                    } else {
                        navController.navigate(Routes.AUTH)
                    }
                },
                onOpenChat = { chatId, peerId ->
                    if (user != null) {
                        navController.navigate(Routes.chat(chatId, peerId))
                    } else {
                        navController.navigate(Routes.AUTH)
                    }
                },
            )
        }
        composable(
            route = Routes.PAYMENT,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType }),
        ) {
            PaymentRoute(
                onBack = { navController.popBackStack() },
                onFlowFinishedNavigateToProfile = {
                    navController.getBackStackEntry(Routes.MAIN).savedStateHandle["pendingMainTab"] = 2
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
