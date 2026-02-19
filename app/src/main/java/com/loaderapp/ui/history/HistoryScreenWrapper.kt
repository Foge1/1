package com.loaderapp.ui.history

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.loaderapp.LoaderApplication
import com.loaderapp.data.model.Order

/**
 * Обёртка вкладки «История».
 * Самостоятельно загружает список завершённых заказов из БД по userId.
 */
@Composable
fun HistoryScreen(userId: Long) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    val orders by app.repository.getOrdersByWorker(userId).collectAsState(initial = emptyList())

    HistoryScreen(
        orders = orders,
        onMenuClick = {},
        onBackClick = {}
    )
}
