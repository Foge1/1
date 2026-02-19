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

/**
 * CompositionLocal для передачи высоты навбара в дочерние экраны.
 *
 * Экраны читают это значение и добавляют его к bottomContentPadding списков,
 * чтобы последняя карточка не была скрыта навбаром при прокрутке в конец.
 * Fade-эффект скрывает карточки визуально раньше, но физически скролл
 * должен давать доступ ко всему контенту.
 */
val LocalBottomNavHeight = compositionLocalOf { 0.dp }

private data class TabConfig(val route: String, val item: BottomNavItem)

@Composable
private fun tabsForRole(role: UserRoleModel): List<TabConfig> {
    val homeIcon = if (role == UserRoleModel.DISPATCHER) Icons.Default.Dashboard
                   else Icons.Default.LocalShipping
    return listOf(
        TabConfig(Route.Home.route,     BottomNavItem(homeIcon,              "Заказы")),
        TabConfig(Route.History.route,  BottomNavItem(Icons.Default.History,  "История")),
        TabConfig(Route.Rating.route,   BottomNavItem(Icons.Default.Star,     "Рейтинг")),
        TabConfig(Route.Profile.route,  BottomNavItem(Icons.Default.Person,   "Профиль")),
        TabConfig(Route.Settings.route, BottomNavItem(Icons.Default.Settings,  "Настройки"))
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
        // Передаём только bottom из innerPadding — он несёт высоту навбара
        // с учётом gesture navigation и системных insets.
        // top = 0: каждый экран сам управляет отступом под свой TopBar
        // через LocalTopBarHeightPx из AppScaffold.
        val bottomNavHeight: Dp = innerPadding.calculateBottomPadding()

        CompositionLocalProvider(LocalBottomNavHeight provides bottomNavHeight) {
            NavHost(
                navController       = navController,
                startDestination    = Route.Home.route,
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                enterTransition     = { fadeIn(tween(200)) },
                exitTransition      = { fadeOut(tween(150)) },
                popEnterTransition  = { fadeIn(tween(200)) },
                popExitTransition   = { fadeOut(tween(150)) }
            ) {
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
                composable(Route.History.route)  { HistoryScreen(userId = user.id) }
                composable(Route.Rating.route)   { RatingScreen() }
                composable(Route.Profile.route)  { ProfileScreen(userId = user.id) }
                composable(Route.Settings.route) {
                    SettingsScreen(onSwitchRole = { sessionViewModel.logout() })
                }
            }
        }
    }
}
