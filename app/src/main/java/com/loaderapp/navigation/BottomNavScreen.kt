package com.loaderapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class описывающий 5 вкладок нижней навигации.
 * Каждый объект содержит route, иконку и подпись.
 */
sealed class BottomNavScreen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Orders : BottomNavScreen(
        route = "bottom_orders",
        label = "Заказы",
        icon = Icons.Default.Assignment
    )

    object History : BottomNavScreen(
        route = "bottom_history",
        label = "История",
        icon = Icons.Default.History
    )

    object Rating : BottomNavScreen(
        route = "bottom_rating",
        label = "Рейтинг",
        icon = Icons.Default.Star
    )

    object Settings : BottomNavScreen(
        route = "bottom_settings",
        label = "Настройки",
        icon = Icons.Default.Settings
    )

    object Profile : BottomNavScreen(
        route = "bottom_profile",
        label = "Профиль",
        icon = Icons.Default.Person
    )

    companion object {
        val items = listOf(Orders, History, Rating, Settings, Profile)
    }
}
