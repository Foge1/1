package com.loaderapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loaderapp.LoaderApplication
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.main.MainScaffoldScreen
import com.loaderapp.ui.order.OrderDetailScreen
import com.loaderapp.ui.splash.SplashScreen
import kotlinx.coroutines.launch

/**
 * Корневой NavGraph приложения.
 *
 * Маршруты:
 *   splash → auth → main/{userId}/{isDispatcher}
 *                           ↓
 *                   MainScaffoldScreen (BottomNav)
 *                           ↓
 *                   order/{orderId}?isDispatcher=...
 *
 * Dispatcher/Loader экраны теперь отображаются внутри MainScaffoldScreen
 * через вложенный BottomNavGraph, а не как отдельные root-маршруты.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Splash.route,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    val scope = rememberCoroutineScope()

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
            SplashScreen(
                onFinished = {
                    onRequestNotificationPermission()
                    scope.launch {
                        val userId = app.userPreferences.getCurrentUserId()
                        if (userId != null) {
                            val user = app.repository.getUserById(userId)
                            if (user != null) {
                                val isDispatcher =
                                    user.role == com.loaderapp.data.model.UserRole.DISPATCHER
                                navController.navigate(
                                    Route.Main.createRoute(userId, isDispatcher)
                                ) {
                                    popUpTo(Route.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Route.Auth.route) {
                                    popUpTo(Route.Splash.route) { inclusive = true }
                                }
                            }
                        } else {
                            navController.navigate(Route.Auth.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // ── Auth (выбор роли) ────────────────────────────────────────────────
        composable(
            route = Route.Auth.route,
            enterTransition = {
                fadeIn(tween(400)) +
                        slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition = { fadeOut(tween(220)) }
        ) {
            RoleSelectionScreen(
                onUserCreated = { newUser ->
                    scope.launch {
                        val userId = app.repository.createUser(newUser)
                        app.userPreferences.setCurrentUserId(userId)
                        val isDispatcher =
                            newUser.role == com.loaderapp.data.model.UserRole.DISPATCHER
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
                    navController.navigate(
                        Route.OrderDetail.createRoute(orderId, isDispatcher)
                    )
                },
                onSwitchRole = {
                    scope.launch {
                        app.userPreferences.clearCurrentUser()
                        navController.navigate(Route.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onDarkThemeChanged = { enabled ->
                    scope.launch { app.userPreferences.setDarkTheme(enabled) }
                }
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
