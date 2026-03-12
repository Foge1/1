package com.loaderapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.features.orders.presentation.navigateToOrderDetail
import com.loaderapp.features.orders.presentation.orderDetailRoute
import com.loaderapp.presentation.session.SessionDestination
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.chat.ChatScreen
import com.loaderapp.ui.main.MainScreen
import com.loaderapp.ui.splash.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Splash.route,
    onRequestNotificationPermission: () -> Unit = {},
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val destination by sessionViewModel.destination.collectAsState()
    val sessionState by sessionViewModel.sessionState.collectAsState()

    LaunchedEffect(destination) {
        when (destination) {
            is SessionDestination.Auth -> {
                val cur = navController.currentDestination?.route
                if (cur != Route.Auth.route && cur != Route.Splash.route) {
                    navController.navigate(Route.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is SessionDestination.Main -> {
                val cur = navController.currentDestination?.route
                if (cur == Route.Auth.route) {
                    navController.navigate(Route.Main.route) {
                        popUpTo(Route.Auth.route) { inclusive = true }
                    }
                }
            }

            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = AppMotion.tweenMedium()) },
        exitTransition = { fadeOut(animationSpec = AppMotion.tweenMedium()) },
    ) {
        composable(
            route = Route.Splash.route,
            enterTransition = { fadeIn(animationSpec = AppMotion.tweenLong()) },
            exitTransition = { fadeOut(animationSpec = AppMotion.tweenLong()) },
        ) {
            SplashScreen(
                isSessionResolved = destination.isResolved,
                onFinished = {
                    onRequestNotificationPermission()
                    when (destination) {
                        is SessionDestination.Main ->
                            navController.navigate(Route.Main.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }

                        is SessionDestination.Auth ->
                            navController.navigate(Route.Auth.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }

                        else -> Unit
                    }
                },
            )
        }

        composable(
            route = Route.Auth.route,
            enterTransition = {
                fadeIn(animationSpec = AppMotion.tweenLong()) +
                    slideInHorizontally(
                        animationSpec = tween(AppMotion.DURATION_LONG, easing = AppMotion.EASING_STANDARD),
                    ) { it / 5 }
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(AppMotion.DURATION_MEDIUM, easing = AppMotion.EASING_STANDARD),
                )
            },
        ) {
            RoleSelectionScreen(
                isLoading = sessionState.isLoading,
                error = sessionState.error,
                onLogin = { name, role -> sessionViewModel.login(name, role) },
            )
        }

        composable(route = Route.Main.route) {
            MainScreen(
                sessionViewModel = sessionViewModel,
                onOrderClick = { orderId, _ ->
                    navController.navigateToOrderDetail(orderId)
                },
            )
        }

        orderDetailRoute(
            onBack = { navController.popBackStack() },
            onOpenChat = { chatOrderId -> navController.navigate(Route.Chat.createRoute(chatOrderId)) },
        )

        composable(
            route = Route.Chat.route,
            arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.LongType }),
        ) {
            val user = sessionState.user ?: return@composable
            ChatScreen(
                userId = user.id,
                userName = user.name,
                userRole = user.role,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
