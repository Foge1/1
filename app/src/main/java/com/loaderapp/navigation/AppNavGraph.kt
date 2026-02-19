package com.loaderapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loaderapp.LoaderApplication
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.order.OrderDetailScreen
import com.loaderapp.ui.splash.SplashScreen
import kotlinx.coroutines.launch

/**
 * Главный NavGraph приложения
 * Использует Navigation Compose для навигации между экранами
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
        enterTransition = { defaultEnterTransition(initialState, targetState) },
        exitTransition = { defaultExitTransition(initialState, targetState) },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        
        // Splash Screen
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
                                when (user.role) {
                                    com.loaderapp.data.model.UserRole.DISPATCHER -> {
                                        navController.navigate(Route.Dispatcher.createRoute(userId)) {
                                            popUpTo(Route.Splash.route) { inclusive = true }
                                        }
                                    }
                                    com.loaderapp.data.model.UserRole.LOADER -> {
                                        navController.navigate(Route.Loader.createRoute(userId)) {
                                            popUpTo(Route.Splash.route) { inclusive = true }
                                        }
                                    }
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
        
        // Auth Screen (Role Selection)
        composable(
            route = Route.Auth.route,
            enterTransition = { 
                fadeIn(tween(400)) + slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition = { fadeOut(tween(220)) }
        ) {
            RoleSelectionScreen(
                onUserCreated = { newUser ->
                    scope.launch {
                        val userId = app.repository.createUser(newUser)
                        app.userPreferences.setCurrentUserId(userId)
                        
                        when (newUser.role) {
                            com.loaderapp.data.model.UserRole.DISPATCHER -> {
                                navController.navigate(Route.Dispatcher.createRoute(userId)) {
                                    popUpTo(Route.Auth.route) { inclusive = true }
                                }
                            }
                            com.loaderapp.data.model.UserRole.LOADER -> {
                                navController.navigate(Route.Loader.createRoute(userId)) {
                                    popUpTo(Route.Auth.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            )
        }
        
        // Dispatcher Screen
        composable(
            route = Route.Dispatcher.route,
            arguments = listOf(
                navArgument(NavArgs.USER_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(NavArgs.USER_ID) ?: return@composable
            val viewModel: DispatcherViewModel = hiltViewModel()
            
            LaunchedEffect(userId) {
                viewModel.initialize(userId)
            }
            
            DispatcherScreen(
                viewModel = viewModel,
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
                },
                onOrderClick = { orderId ->
                    navController.navigate(Route.OrderDetail.createRoute(orderId, isDispatcher = true))
                }
            )
        }
        
        // Loader Screen
        composable(
            route = Route.Loader.route,
            arguments = listOf(
                navArgument(NavArgs.USER_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(NavArgs.USER_ID) ?: return@composable
            val viewModel: LoaderViewModel = hiltViewModel()
            
            LaunchedEffect(userId) {
                viewModel.initialize(userId)
            }
            
            LoaderScreen(
                viewModel = viewModel,
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
                },
                onOrderClick = { orderId ->
                    navController.navigate(Route.OrderDetail.createRoute(orderId, isDispatcher = false))
                }
            )
        }
        
        // Order Detail Screen
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
                fadeIn(tween(280)) + slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 5 }
            },
            exitTransition = {
                fadeOut(tween(200)) + slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it / 5 }
            }
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong(NavArgs.ORDER_ID) ?: return@composable
            val isDispatcher = backStackEntry.arguments?.getBoolean(NavArgs.IS_DISPATCHER) ?: false
            
            OrderDetailScreen(
                orderId = orderId,
                isDispatcher = isDispatcher,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Дефолтная анимация входа
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry
): EnterTransition {
    return fadeIn(tween(300))
}

/**
 * Дефолтная анимация выхода
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry
): ExitTransition {
    return fadeOut(tween(200))
}

/**
 * Анимация входа при возврате назад
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition(): EnterTransition {
    return fadeIn(tween(240))
}

/**
 * Анимация выхода при возврате назад
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition(): ExitTransition {
    return fadeOut(tween(200))
}
