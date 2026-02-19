package com.loaderapp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loaderapp.navigation.BottomNavGraph
import com.loaderapp.navigation.BottomNavScreen
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem

/**
 * Главный Scaffold с Bottom Navigation.
 * Создаёт собственный вложенный NavController для вкладок,
 * чтобы не смешивать его с корневым NavController.
 */
@Composable
fun MainScaffoldScreen(
    userId: Long,
    isDispatcher: Boolean,
    onNavigateToOrderDetail: (Long) -> Unit,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    val bottomNavController: NavHostController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedIndex = BottomNavScreen.items.indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            AppBottomBar(
                items = BottomNavScreen.items.map { screen ->
                    BottomNavItem(
                        icon = screen.icon,
                        label = screen.label
                    )
                },
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    val screen = BottomNavScreen.items[index]
                    if (screen.route != currentRoute) {
                        bottomNavController.navigate(screen.route) {
                            popUpTo(BottomNavScreen.Orders.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        BottomNavGraph(
            navController = bottomNavController,
            userId = userId,
            isDispatcher = isDispatcher,
            onNavigateToOrderDetail = onNavigateToOrderDetail,
            onSwitchRole = onSwitchRole,
            onDarkThemeChanged = onDarkThemeChanged,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
