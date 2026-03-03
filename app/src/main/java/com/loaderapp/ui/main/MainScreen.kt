package com.loaderapp.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.orders.presentation.OrdersViewModel
import com.loaderapp.features.orders.presentation.ResponsesViewModel
import com.loaderapp.navigation.Route
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.dispatcher.CreateOrderScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.dispatcher.ResponsesScreen
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen

val LocalBottomNavHeight = compositionLocalOf { 0.dp }

private data class TabConfig(
    val route: String,
    val item: BottomNavItem,
)

private val FULLSCREEN_ROUTES: Set<String> = setOf(Route.CreateOrder.route)

@Composable
private fun tabsForRole(
    role: UserRoleModel,
    responsesBadgeCount: Int,
): List<TabConfig> {
    val homeIcon = if (role == UserRoleModel.DISPATCHER) Icons.Default.Dashboard else Icons.Default.LocalShipping
    val middleTab =
        if (role == UserRoleModel.DISPATCHER) {
            TabConfig(Route.Responses.route, BottomNavItem(Icons.Default.History, "Отклики", badgeCount = responsesBadgeCount))
        } else {
            TabConfig(Route.History.route, BottomNavItem(Icons.Default.History, "История"))
        }
    return listOf(
        TabConfig(Route.Home.route, BottomNavItem(homeIcon, "Заказы")),
        middleTab,
        TabConfig(Route.Rating.route, BottomNavItem(Icons.Default.Star, "Рейтинг")),
        TabConfig(Route.Profile.route, BottomNavItem(Icons.Default.Person, "Профиль")),
        TabConfig(Route.Settings.route, BottomNavItem(Icons.Default.Settings, "Настройки")),
    )
}

@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel,
    onOrderClick: (orderId: Long, isDispatcher: Boolean) -> Unit,
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val user = sessionState.user ?: return
    val navController = rememberNavController()

    val dispatcherOrdersVm: OrdersViewModel? = if (user.role == UserRoleModel.DISPATCHER) hiltViewModel() else null
    val dispatcherOrdersState by dispatcherOrdersVm?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val tabs = tabsForRole(user.role, dispatcherOrdersState?.responsesBadge?.totalResponses ?: 0)
    val isFullscreen = currentRoute in FULLSCREEN_ROUTES
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            MainBottomBar(
                isFullscreen = isFullscreen,
                tabs = tabs,
                selectedIndex = selectedIndex,
                currentRoute = currentRoute,
                navController = navController,
            )
        },
    ) { innerPadding ->
        val bottomNavHeight = if (!isFullscreen) innerPadding.calculateBottomPadding() else 0.dp
        CompositionLocalProvider(LocalBottomNavHeight provides bottomNavHeight) {
            MainNavHost(
                navController = navController,
                topPadding = innerPadding.calculateTopPadding(),
                userRole = user.role,
                userId = user.id,
                dispatcherOrdersVm = dispatcherOrdersVm,
                onOrderClick = onOrderClick,
                onSwitchRole = { sessionViewModel.logout() },
            )
        }
    }
}

@Composable
private fun MainBottomBar(
    isFullscreen: Boolean,
    tabs: List<TabConfig>,
    selectedIndex: Int,
    currentRoute: String?,
    navController: NavHostController,
) {
    if (isFullscreen) return
    AppBottomBar(
        items = tabs.map { it.item },
        selectedIndex = selectedIndex,
        onItemSelected = { index ->
            val route = tabs[index].route
            if (route != currentRoute) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
    )
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    topPadding: androidx.compose.ui.unit.Dp,
    userRole: UserRoleModel,
    userId: Long,
    dispatcherOrdersVm: OrdersViewModel?,
    onOrderClick: (Long, Boolean) -> Unit,
    onSwitchRole: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = Modifier.fillMaxSize().padding(top = topPadding),
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Route.Home.route) {
            HomeRoute(
                role = userRole,
                dispatcherOrdersVm = dispatcherOrdersVm,
                onOrderClick = onOrderClick,
                onNavigateToCreateOrder = {
                    if (navController.currentDestination?.route != Route.CreateOrder.route) {
                        navController.navigate(Route.CreateOrder.route) { launchSingleTop = true }
                    }
                },
            )
        }

        composable(Route.History.route) {
            HistoryScreen(
                userId = userId,
                userRole = userRole,
                onOrderClick = { orderId -> onOrderClick(orderId, userRole == UserRoleModel.DISPATCHER) },
            )
        }

        composable(Route.Responses.route) {
            if (userRole == UserRoleModel.DISPATCHER) {
                val responsesVm: ResponsesViewModel = hiltViewModel()
                ResponsesScreen(viewModel = responsesVm)
            } else {
                HistoryScreen(
                    userId = userId,
                    userRole = userRole,
                    onOrderClick = { orderId -> onOrderClick(orderId, false) },
                )
            }
        }

        composable(Route.Rating.route) { RatingScreen() }
        composable(Route.Profile.route) { ProfileScreen(userId = userId) }
        composable(Route.Settings.route) { SettingsScreen(onSwitchRole = onSwitchRole) }

        composable(
            route = Route.CreateOrder.route,
            enterTransition = { slideInVertically(tween(320)) { it / 6 } + fadeIn(tween(260)) },
            exitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(200)) },
        ) {
            CreateOrderScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeRoute(
    role: UserRoleModel,
    dispatcherOrdersVm: OrdersViewModel?,
    onOrderClick: (Long, Boolean) -> Unit,
    onNavigateToCreateOrder: () -> Unit,
) {
    when (role) {
        UserRoleModel.DISPATCHER -> {
            val ordersVm = dispatcherOrdersVm ?: hiltViewModel<OrdersViewModel>()
            DispatcherScreen(
                viewModel = ordersVm,
                onOrderClick = { orderId -> onOrderClick(orderId, true) },
                onNavigateToCreateOrder = onNavigateToCreateOrder,
            )
        }

        UserRoleModel.LOADER -> {
            val ordersVm: OrdersViewModel = hiltViewModel()
            LoaderScreen(
                viewModel = ordersVm,
                onOrderClick = { orderId -> onOrderClick(orderId, false) },
            )
        }
    }
}
