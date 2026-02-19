package com.loaderapp.ui.rating

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.loaderapp.LoaderApplication
import com.loaderapp.data.model.OrderStatus

/**
 * Обёртка вкладки «Рейтинг».
 * Загружает данные пользователя и статистику из БД.
 */
@Composable
fun RatingScreen(userId: Long) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }

    var userName by remember { mutableStateOf("") }
    var userRating by remember { mutableStateOf(5.0) }
    val orders by app.repository.getOrdersByWorker(userId).collectAsState(initial = emptyList())

    LaunchedEffect(userId) {
        val user = app.repository.getUserById(userId)
        userName = user?.name ?: ""
        userRating = user?.rating ?: 5.0
    }

    val completed = orders.filter { it.status == OrderStatus.COMPLETED }
    val totalEarnings = completed.sumOf { it.pricePerHour * it.estimatedHours }

    RatingScreen(
        userName = userName,
        userRating = userRating,
        onMenuClick = {},
        onBackClick = {},
        completedCount = completed.size,
        totalEarnings = totalEarnings,
        averageRating = userRating.toFloat()
    )
}
