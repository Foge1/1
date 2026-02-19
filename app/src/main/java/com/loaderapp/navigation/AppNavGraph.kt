package com.loaderapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.presentation.session.CreateUserViewModel
import com.loaderapp.presentation.session.SessionDestination
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.auth.RoleSelectionScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.order.OrderDetailScreen
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.ui.splash.SplashScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Splash.route,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val destination by sessionViewModel.destination.collectAsState()
    val scope = rememberCoroutineScope()

    // Реагируем на clearSession() — навигируем на Auth с очисткой стека
    LaunchedEffect(destination) {
        if (destination is SessionDestination.Auth) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Route.Auth.route && currentRoute != Route.Splash.route) {
                navController.navigate(Route.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(200)) }
    ) {

        // ── Splash ───────────────────────────────────────────────────────────
        composable(
            route = Route.Splash.route,
            enterTransition = { fadeIn(tween(500, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(350)) }
        ) {
            SplashScreen(
                // Race condition fix: Splash ждёт пока SessionViewModel прочитает DataStore
                isSessionResolved = destination.isResolved,
                onFinished = {
                    onRequestNotificationPermission()
                    when (val dest = destination) {
                        is SessionDestination.Loading    -> { /* не должны сюда попасть */ }
                        is SessionDestination.Dispatcher ->
                            navController.navigate(Route.Dispatcher.createRoute(dest.userId)) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        is SessionDestination.Loader     ->
                            navController.navigate(Route.Loader.createRoute(dest.userId)) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        is SessionDestination.Auth       ->
                            navController.navigate(Route.Auth.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                    }
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
            val createUserViewModel: CreateUserViewModel = hiltViewModel()

            RoleSelectionScreen(
                onUserCreated = { newUser ->
                    scope.launch {
                        val userId = createUserViewModel.createUser(newUser)
                        if (userId != null) {
                            sessionViewModel.onUserCreated(newUser, userId)
                            when (newUser.role) {
                                com.loaderapp.data.model.UserRole.DISPATCHER ->
                                    navController.navigate(Route.Dispatcher.createRoute(userId)) {
                                        popUpTo(Route.Auth.route) { inclusive = true }
                                    }
                                com.loaderapp.data.model.UserRole.LOADER ->
                                    navController.navigate(Route.Loader.createRoute(userId)) {
                                        popUpTo(Route.Auth.route) { inclusive = true }
                                    }
                            }
                        }
                    }
                }
            )
        }

        // ── Dispatcher ───────────────────────────────────────────────────────
        composable(
            route = Route.Dispatcher.route,
            arguments = listOf(navArgument(NavArgs.USER_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(NavArgs.USER_ID) ?: return@composable
            val viewModel: DispatcherViewModel = hiltViewModel()
            LaunchedEffect(userId) { viewModel.initialize(userId) }

            DispatcherScreen(
                viewModel = viewModel,
                onSwitchRole = { sessionViewModel.clearSession() },
                onDarkThemeChanged = { enabled -> sessionViewModel.setDarkTheme(enabled) },
                onOrderClick = { orderId ->
                    navController.navigate(Route.OrderDetail.createRoute(orderId, isDispatcher = true))
                }
            )
        }

        // ── Loader ───────────────────────────────────────────────────────────
        composable(
            route = Route.Loader.route,
            arguments = listOf(navArgument(NavArgs.USER_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong(NavArgs.USER_ID) ?: return@composable
            val viewModel: LoaderViewModel = hiltViewModel()
            LaunchedEffect(userId) { viewModel.initialize(userId) }

            LoaderScreen(
                viewModel = viewModel,
                onSwitchRole = { sessionViewModel.clearSession() },
                onDarkThemeChanged = { enabled -> sessionViewModel.setDarkTheme(enabled) },
                onOrderClick = { orderId ->
                    navController.navigate(Route.OrderDetail.createRoute(orderId, isDispatcher = false))
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
            val orderId = backStackEntry.arguments?.getLong(NavArgs.ORDER_ID) ?: return@composable
            val isDispatcher = backStackEntry.arguments?.getBoolean(NavArgs.IS_DISPATCHER) ?: false
            val viewModel: OrderDetailViewModel = hiltViewModel()
            LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

            OrderDetailScreen(
                viewModel = viewModel,
                isDispatcher = isDispatcher,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
