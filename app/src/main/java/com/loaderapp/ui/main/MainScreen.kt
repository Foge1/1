package com.loaderapp.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.navigation.Route
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen

// ── Конфигурация вкладок ──────────────────────────────────────────────────────

/**
 * Описание одной вкладки нижней панели.
 * Добавить вкладку = добавить одну запись в [tabsForRole].
 */
private data class TabConfig(
    val route: String,
    val item: BottomNavItem
)

/**
 * Централизованная конфигурация вкладок по роли.
 * Диспетчер и Грузчик могут иметь разные наборы вкладок.
 */
@Composable
private fun tabsForRole(role: UserRoleModel): List<TabConfig> {
    val homeLabel = if (role == UserRoleModel.DISPATCHER) "Заказы" else "Заказы"
    val homeIcon  = if (role == UserRoleModel.DISPATCHER) Icons.Default.Dashboard
                    else Icons.Default.LocalShipping

    return listOf(
        TabConfig(Route.Home.route,     BottomNavItem(homeIcon,                 homeLabel)),
        TabConfig(Route.History.route,  BottomNavItem(Icons.Default.History,    "История")),
        TabConfig(Route.Rating.route,   BottomNavItem(Icons.Default.Star,       "Рейтинг")),
        TabConfig(Route.Profile.route,  BottomNavItem(Icons.Default.Person,     "Профиль")),
        TabConfig(Route.Settings.route, BottomNavItem(Icons.Default.Settings,   "Настройки"))
    )
}

// ── Главный экран ─────────────────────────────────────────────────────────────

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

    val tabs = tabsForRole(user.role)

    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            AppBottomBar(
                items         = tabs.map { it.item },
                selectedIndex = selectedIndex,
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
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Route.Home.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(tween(200)) },
            exitTransition   = { fadeOut(tween(150)) },
            popEnterTransition  = { fadeIn(tween(200)) },
            popExitTransition   = { fadeOut(tween(150)) }
        ) {

            // ── Заказы / Биржа ───────────────────────────────────────────────
            composable(Route.Home.route) {
                when (user.role) {
                    UserRoleModel.DISPATCHER -> {
                        val vm: DispatcherViewModel = hiltViewModel()
                        LaunchedEffect(user.id) { vm.initialize(user.id) }
                        DispatcherScreen(
                            viewModel    = vm,
                            onOrderClick = { orderId -> onOrderClick(orderId, true) }
                        )
                    }
                    UserRoleModel.LOADER -> {
                        val vm: LoaderViewModel = hiltViewModel()
                        LaunchedEffect(user.id) { vm.initialize(user.id) }
                        LoaderScreen(
                            viewModel    = vm,
                            onOrderClick = { orderId -> onOrderClick(orderId, false) }
                        )
                    }
                }
            }

            // ── История ──────────────────────────────────────────────────────
            composable(Route.History.route) {
                HistoryScreen(userId = user.id)
            }

            // ── Рейтинг ──────────────────────────────────────────────────────
            composable(Route.Rating.route) {
                RatingScreen()
            }

            // ── Профиль ──────────────────────────────────────────────────────
            composable(Route.Profile.route) {
                ProfileScreen(userId = user.id)
            }

            // ── Настройки ────────────────────────────────────────────────────
            composable(Route.Settings.route) {
                SettingsScreen(
                    onSwitchRole = { sessionViewModel.logout() }
                )
            }
        }
    }
}
