package com.loaderapp.ui.main

import androidx.compose.animation.core.*
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
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.presentation.session.SessionViewModel
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.loader.LoaderScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.settings.SettingsScreen
import com.loaderapp.navigation.Route

private const val TAB_HOME     = 0
private const val TAB_PROFILE  = 1
private const val TAB_SETTINGS = 2

@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel,
    onOrderClick: (orderId: Long, isDispatcher: Boolean) -> Unit
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val user = sessionState.user ?: return   // защита: если нет юзера — ничего не рендерим

    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    val selectedIndex = when (currentRoute) {
        Route.Home.route     -> TAB_HOME
        Route.Profile.route  -> TAB_PROFILE
        Route.Settings.route -> TAB_SETTINGS
        else                 -> TAB_HOME
    }

    val bottomItems = listOf(
        BottomNavItem(
            icon  = if (user.role == UserRoleModel.DISPATCHER) Icons.Default.Dashboard
                    else Icons.Default.LocalShipping,
            label = if (user.role == UserRoleModel.DISPATCHER) "Заказы" else "Биржа"
        ),
        BottomNavItem(icon = Icons.Default.Person,   label = "Профиль"),
        BottomNavItem(icon = Icons.Default.Settings, label = "Настройки")
    )

    Scaffold(
        bottomBar = {
            AppBottomBar(
                items         = bottomItems,
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    val route = when (index) {
                        TAB_HOME     -> Route.Home.route
                        TAB_PROFILE  -> Route.Profile.route
                        TAB_SETTINGS -> Route.Settings.route
                        else         -> Route.Home.route
                    }
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
            enterTransition  = { fadeIn(tween(220)) },
            exitTransition   = { fadeOut(tween(180)) }
        ) {

            // ── Home (роль определяет контент) ─────────────────────────────
            composable(Route.Home.route) {
                when (user.role) {
                    UserRoleModel.DISPATCHER -> {
                        val vm: DispatcherViewModel = hiltViewModel()
                        LaunchedEffect(user.id) { vm.initialize(user.id) }
                        DispatcherScreen(
                            viewModel   = vm,
                            onOrderClick = { orderId ->
                                onOrderClick(orderId, true)
                            }
                        )
                    }
                    UserRoleModel.LOADER -> {
                        val vm: LoaderViewModel = hiltViewModel()
                        LaunchedEffect(user.id) { vm.initialize(user.id) }
                        LoaderScreen(
                            viewModel    = vm,
                            onOrderClick = { orderId ->
                                onOrderClick(orderId, false)
                            }
                        )
                    }
                }
            }

            // ── Profile ─────────────────────────────────────────────────────
            composable(Route.Profile.route) {
                ProfileScreen(userId = user.id)
            }

            // ── Settings ────────────────────────────────────────────────────
            composable(Route.Settings.route) {
                SettingsScreen(
                    onSwitchRole = { sessionViewModel.logout() }
                )
            }
        }
    }
}
