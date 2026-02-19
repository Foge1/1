package com.loaderapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.presentation.splash.SplashDestination
import com.loaderapp.presentation.splash.SplashViewModel
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.main.MainScaffoldScreen
import com.loaderapp.ui.order.OrderDetailScreen
import com.loaderapp.ui.splash.SplashScreen

/**
 * Корневой NavGraph.
 *
 * Вся бизнес-логика (кто авторизован, какая роль) вынесена
 * в SplashViewModel — NavGraph только навигирует.
 *
 * Маршруты:
 *   splash → auth → main/{userId}/{isDispatcher}
 *                         ↓
 *               MainScaffoldScreen (BottomNav)
 *                         ↓
 *               order/{orderId}?isDispatcher=...
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Splash.route,
    onRequestNotificationPermission: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(200)) }
    ) {

        // ── Splash ──────────────────────────────────────────────────────────
        composable(
            route = Route.Splash.route,
            enterTransition = { fadeIn(tween(500, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(350)) }
        ) {
            val viewModel: SplashViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                viewModel.destination.collect { dest ->
                    when (dest) {
                        is SplashDestination.Main -> {
                            navController.navigate(
                                Route.Main.createRoute(dest.userId, dest.isDispatcher)
                            ) { popUpTo(Route.Splash.route) { inclusive = true } }
                        }
                        SplashDestination.Auth -> {
                            navController.navigate(Route.Auth.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        }
                    }
                }
            }

            SplashScreen(
                onFinished = {
                    onRequestNotificationPermission()
                    viewModel.resolveStartDestination()
                }
            )
        }

        // ── Auth ─────────────────────────────────────────────────────────────
        composable(
            route = Route.Auth.route,
            enterTransition = {
                fadeIn(tween(400)) +
                        slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition = { fadeOut(tween(220)) }
        ) {
            val viewModel: com.loaderapp.presentation.auth.AuthViewModel = hiltViewModel()

            RoleSelectionScreen(
                onUserCreated = { newUser ->
                    viewModel.createUser(newUser) { userId, isDispatcher ->
                        navController.navigate(Route.Main.createRoute(userId, isDispatcher)) {
                            popUpTo(Route.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Main (Scaffold + BottomNav) ──────────────────────────────────────
        composable(
            route = Route.Main.route,
            arguments = listOf(
                navArgument(NavArgs.USER_ID) { type = NavType.LongType },
                navArgument(NavArgs.IS_DISPATCHER) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(NavArgs.USER_ID) ?: return@composable
            val isDispatcher =
                backStackEntry.arguments?.getBoolean(NavArgs.IS_DISPATCHER) ?: false

            MainScaffoldScreen(
                userId = userId,
                isDispatcher = isDispatcher,
                onNavigateToOrderDetail = { orderId ->
                    navController.navigate(Route.OrderDetail.createRoute(orderId, isDispatcher))
                },
                onSwitchRole = {
                    navController.navigate(Route.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDarkThemeChanged = {}
            )
        }

        // ── Order Detail ─────────────────────────────────────────────────────
        composable(
            route = Route.OrderDetail.route,
            arguments = listOf(
                navArgument(NavArgs.ORDER_ID) { type = NavType.LongType },
                navArgument(NavArgs.IS_DISPATCHER) {
                    type = NavType.BoolType
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
        ) { backStackEntry ->
            val orderId =
                backStackEntry.arguments?.getLong(NavArgs.ORDER_ID) ?: return@composable
            val isDispatcher =
                backStackEntry.arguments?.getBoolean(NavArgs.IS_DISPATCHER) ?: false

            OrderDetailScreen(
                orderId = orderId,
                isDispatcher = isDispatcher,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
