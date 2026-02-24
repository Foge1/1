package com.loaderapp.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.navigation.Route
import com.loaderapp.features.orders.ui.OrdersViewModel
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.dispatcher.CreateOrderScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.features.orders.ui.ResponsesViewModel
import com.loaderapp.ui.dispatcher.ResponsesScreen
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen

/**
 * CompositionLocal для передачи высоты Bottom Navigation в дочерние экраны.
 *
 * Экраны используют это значение для нижнего contentPadding в списках,
 * чтобы последняя карточка не скрывалась под навбаром при прокрутке.
 */
val LocalBottomNavHeight = compositionLocalOf { 0.dp }

private data class TabConfig(val route: String, val item: BottomNavItem)

/**
 * Декларативное множество маршрутов, скрывающих Bottom Navigation.
 */
private val FULLSCREEN_ROUTES: Set<String> = setOf(Route.CreateOrder.route)

@Composable
private fun tabsForRole(role: UserRoleModel, responsesBadgeCount: Int): List<TabConfig> {
    val homeIcon = if (role == UserRoleModel.DISPATCHER) Icons.Default.Dashboard
    else Icons.Default.LocalShipping
    val middleTab = if (role == UserRoleModel.DISPATCHER) {
        TabConfig(Route.Responses.route, BottomNavItem(Icons.Default.History, "Отклики", badgeCount = responsesBadgeCount))
    } else {
        TabConfig(Route.History.route, BottomNavItem(Icons.Default.History, "История"))
    }
    return listOf(
        TabConfig(Route.Home.route, BottomNavItem(homeIcon, "Заказы")),
        middleTab,
        TabConfig(Route.Rating.route, BottomNavItem(Icons.Default.Star, "Рейтинг")),
        TabConfig(Route.Profile.route, BottomNavItem(Icons.Default.Person, "Профиль")),
        TabConfig(Route.Settings.route, BottomNavItem(Icons.Default.Settings, "Настройки"))
    )
}

@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel,
    onOrderClick: (orderId: Long, isDispatcher: Boolean) -> Unit
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val user = sessionState.user ?: return

    val navController = rememberNavController()

    val dispatcherOrdersVm: OrdersViewModel? = if (user.role == UserRoleModel.DISPATCHER) hiltViewModel() else null
    val dispatcherOrdersState by dispatcherOrdersVm?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route
    val tabs          = tabsForRole(user.role, dispatcherOrdersState?.responsesBadgeCount ?: 0)

    val isFullscreen  = currentRoute in FULLSCREEN_ROUTES
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            if (!isFullscreen) {
                AppBottomBar(
                    items          = tabs.map { it.item },
                    selectedIndex  = selectedIndex,
                    onItemSelected = { index ->
                        val route = tabs[index].route
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val bottomNavHeight: Dp =
            if (!isFullscreen) innerPadding.calculateBottomPadding() else 0.dp

        CompositionLocalProvider(LocalBottomNavHeight provides bottomNavHeight) {
            NavHost(
                navController      = navController,
                startDestination   = Route.Home.route,
                modifier           = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                enterTransition    = { fadeIn(tween(200)) },
                exitTransition     = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition  = { fadeOut(tween(150)) }
            ) {

                composable(Route.Home.route) {
                    // Оба экрана — и лоадера, и диспетчера — работают
                    // через единый OrdersViewModel (новая модель).
                    // ObserveOrderUiModelsUseCase сам фильтрует заказы по роли актора.
                    when (user.role) {
                        UserRoleModel.DISPATCHER -> {
                            val ordersVm = dispatcherOrdersVm ?: hiltViewModel<OrdersViewModel>()
                            DispatcherScreen(
                                viewModel = ordersVm,
                                onOrderClick = { orderId -> onOrderClick(orderId, true) },
                                onNavigateToCreateOrder = {
                                    if (navController.currentDestination?.route != Route.CreateOrder.route) {
                                        navController.navigate(Route.CreateOrder.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                        UserRoleModel.LOADER -> {
                            val ordersVm: OrdersViewModel = hiltViewModel()
                            LoaderScreen(
                                viewModel    = ordersVm,
                                onOrderClick = { orderId -> onOrderClick(orderId, false) }
                            )
                        }
                    }
                }

                composable(Route.History.route) {
                    HistoryScreen(
                        userId = user.id,
                        userRole = user.role,
                        onOrderClick = { orderId ->
                            onOrderClick(orderId, user.role == UserRoleModel.DISPATCHER)
                        }
                    )
                }

                composable(Route.Responses.route) {
                    if (user.role == UserRoleModel.DISPATCHER) {
                        val responsesVm: ResponsesViewModel = hiltViewModel()
                        ResponsesScreen(viewModel = responsesVm)
                    } else {
                        HistoryScreen(
                            userId = user.id,
                            userRole = user.role,
                            onOrderClick = { orderId -> onOrderClick(orderId, false) }
                        )
                    }
                }

                composable(Route.Rating.route)   { RatingScreen() }
                composable(Route.Profile.route)  { ProfileScreen(userId = user.id) }
                composable(Route.Settings.route) {
                    SettingsScreen(onSwitchRole = { sessionViewModel.logout() })
                }

                composable(
                    route           = Route.CreateOrder.route,
                    enterTransition = {
                        slideInVertically(tween(320)) { it / 6 } + fadeIn(tween(260))
                    },
                    exitTransition  = {
                        slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(200))
                    }
                ) {
                    CreateOrderScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
