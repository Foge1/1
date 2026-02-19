package com.loaderapp.ui.profile

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.loaderapp.LoaderApplication
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import kotlinx.coroutines.launch

/**
 * Обёртка вкладки «Профиль».
 * Загружает пользователя из БД и передаёт статистику в ProfileScreen.
 */
@Composable
fun ProfileScreen(userId: Long) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    val orders by app.repository.getOrdersByWorker(userId).collectAsState(initial = emptyList())
    val dispatcherOrders by app.repository.getOrdersByDispatcher(userId).collectAsState(initial = emptyList())

    LaunchedEffect(userId) {
        user = app.repository.getUserById(userId)
    }

    val currentUser = user ?: return

    val completed = orders.filter { it.status == OrderStatus.COMPLETED }
    val totalEarnings = completed.sumOf { it.pricePerHour * it.estimatedHours }
    val dispatcherCompleted = dispatcherOrders.count { it.status == OrderStatus.COMPLETED }
    val dispatcherActive = dispatcherOrders.count {
        it.status == OrderStatus.AVAILABLE || it.status == OrderStatus.TAKEN
    }

    ProfileScreen(
        user = currentUser,
        completedCount = completed.size,
        totalEarnings = totalEarnings,
        averageRating = currentUser.rating.toFloat(),
        dispatcherCompletedCount = dispatcherCompleted,
        dispatcherActiveCount = dispatcherActive,
        onMenuClick = {},
        onSaveProfile = { name, phone, birthDate ->
            scope.launch {
                app.repository.updateUser(
                    currentUser.copy(name = name, phone = phone, birthDate = birthDate)
                )
            }
        }
    )
}
