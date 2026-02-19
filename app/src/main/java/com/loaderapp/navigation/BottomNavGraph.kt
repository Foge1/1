package com.loaderapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.orders.OrdersScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen

/**
 * Вложенный NavHost для Bottom Navigation.
 * Каждая вкладка — отдельный composable с сохранением состояния (restoreState = true).
 * launchSingleTop = true исключает дубликаты в back stack.
 */
@Composable
fun BottomNavGraph(
    navController: NavHostController,
    userId: Long,
    isDispatcher: Boolean,
    onNavigateToOrderDetail: (Long) -> Unit,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Orders.route,
        modifier = modifier
    ) {
        composable(BottomNavScreen.Orders.route) {
            OrdersScreen(
                userId = userId,
                isDispatcher = isDispatcher,
                onOrderClick = onNavigateToOrderDetail
            )
        }

        composable(BottomNavScreen.History.route) {
            HistoryScreen(userId = userId)
        }

        composable(BottomNavScreen.Rating.route) {
            RatingScreen(userId = userId)
        }

        composable(BottomNavScreen.Settings.route) {
            SettingsScreen(
                onSwitchRole = onSwitchRole,
                onDarkThemeChanged = onDarkThemeChanged
            )
        }

        composable(BottomNavScreen.Profile.route) {
            ProfileScreen(userId = userId)
        }
    }
}
