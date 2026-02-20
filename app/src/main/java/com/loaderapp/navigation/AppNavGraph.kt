package com.loaderapp.navigation

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.presentation.session.SessionDestination
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.main.MainScreen
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.order.OrderDetailScreen
import com.loaderapp.ui.chat.ChatScreen
import com.loaderapp.ui.splash.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Splash.route,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val destination by sessionViewModel.destination.collectAsState()
    val sessionState by sessionViewModel.sessionState.collectAsState()

    // Глобальная реакция на logout → возврат на Auth с очисткой стека
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
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { fadeIn(tween(300)) },
        exitTransition   = { fadeOut(tween(200)) }
    ) {

        // ── Splash ──────────────────────────────────────────────────────────
        composable(
            route = Route.Splash.route,
            enterTransition = { fadeIn(tween(500, easing = FastOutSlowInEasing)) },
            exitTransition  = { fadeOut(tween(350)) }
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
                }
            )
        }

        // ── Auth ─────────────────────────────────────────────────────────────
        composable(
            route           = Route.Auth.route,
            enterTransition = {
                fadeIn(tween(400)) +
                slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition  = { fadeOut(tween(220)) }
        ) {
            RoleSelectionScreen(
                isLoading = sessionState.isLoading,
                error     = sessionState.error,
                onLogin   = { name, role -> sessionViewModel.login(name, role) }
            )
        }


        // ── Main (единый для всех ролей) ─────────────────────────────────────
        composable(route = Route.Main.route) {
            MainScreen(
                sessionViewModel = sessionViewModel,
                onOrderClick     = { orderId, isDispatcher ->
                    navController.navigate(
                        Route.OrderDetail.createRoute(orderId, isDispatcher)
                    )
                }
            )
        }

        // ── Order Detail ─────────────────────────────────────────────────────
        composable(
            route     = Route.OrderDetail.route,
            arguments = listOf(
                navArgument(NavArgs.ORDER_ID)      { type = NavType.LongType },
                navArgument(NavArgs.IS_DISPATCHER) {
                    type         = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = {
                fadeIn(tween(280)) +
                slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition = {
                fadeOut(tween(200)) +
                slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it / 5 }
            }
        ) { _ ->
            val vm: OrderDetailViewModel = hiltViewModel()
            // No LaunchedEffect needed: OrderDetailViewModel reads orderId from
            // SavedStateHandle in init{} and starts loading automatically.
            OrderDetailScreen(
                viewModel    = vm,
                onBack       = { navController.popBackStack() },
                onOpenChat   = { chatOrderId -> navController.navigate(Route.Chat.createRoute(chatOrderId)) }
            )
        }

        composable(
            route = Route.Chat.route,
            arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.LongType })
        ) {
            val user = sessionState.user ?: return@composable
            ChatScreen(
                userId = user.id,
                userName = user.name,
                userRole = user.role,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
