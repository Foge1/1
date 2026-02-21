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
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.features.orders.ui.OrdersViewModel
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.dispatcher.CreateOrderScreen
import com.loaderapp.ui.dispatcher.DispatcherScreen
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
 * Добавление нового полноэкранного экрана = одна строка здесь.
 * Никакой условной логики в коде.
 */
private val FULLSCREEN_ROUTES: Set<String> = setOf(
    Route.CreateOrder.route
)

@Composable
private fun tabsForRole(role: UserRoleModel): List<TabConfig> {
    val homeIcon = if (role == UserRoleModel.DISPATCHER) Icons.Default.Dashboard
                   else Icons.Default.LocalShipping
    return listOf(
        TabConfig(Route.Home.route,     BottomNavItem(homeIcon,               "Заказы")),
        TabConfig(Route.History.route,  BottomNavItem(Icons.Default.History,  "История")),
        TabConfig(Route.Rating.route,   BottomNavItem(Icons.Default.Star,     "Рейтинг")),
        TabConfig(Route.Profile.route,  BottomNavItem(Icons.Default.Person,   "Профиль")),
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
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route
    val tabs          = tabsForRole(user.role)

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
                    when (user.role) {
                        UserRoleModel.DISPATCHER -> {
                            val vm: DispatcherViewModel = hiltViewModel()
                            LaunchedEffect(user.id) { vm.initialize(user.id) }
                            DispatcherScreen(
                                viewModel               = vm,
                                onOrderClick            = { orderId -> onOrderClick(orderId, true) },
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
                            val vm: OrdersViewModel = hiltViewModel()
                            LoaderScreen(
                                viewModel    = vm,
                                onOrderClick = { orderId -> onOrderClick(orderId, false) }
                            )
                        }
                    }
                }

                composable(Route.History.route) {
                    HistoryScreen(
                        userId       = user.id,
                        userRole     = user.role,
                        onOrderClick = { orderId ->
                            onOrderClick(orderId, user.role == UserRoleModel.DISPATCHER)
                        }
                    )
                }

                composable(Route.Rating.route)   { RatingScreen() }
                composable(Route.Profile.route)  { ProfileScreen(userId = user.id) }
                composable(Route.Settings.route) {
                    SettingsScreen(onSwitchRole = { sessionViewModel.logout() })
                }

                // ── Создание заказа ──────────────────────────────────────────
                // CreateOrderScreen имеет собственный CreateOrderViewModel,
                // получаемый через стандартный hiltViewModel() без хаков.
                // После успешного сохранения VM эмитит NavigationEvent.NavigateUp,
                // экран вызывает onBack → popBackStack.
                // DispatcherViewModel узнаёт о новом заказе автоматически
                // через Room Flow — без прямой связи между VM.
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
                        dispatcherId = user.id,
                        onBack       = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
