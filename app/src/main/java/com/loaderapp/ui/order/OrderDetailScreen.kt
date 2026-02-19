package com.loaderapp.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.order.OrderDetailViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    isDispatcher: Boolean,
    onBack: () -> Unit
) {
    val orderState  by viewModel.orderState.collectAsState()
    val workerCount by viewModel.workerCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (orderState) {
                            is UiState.Success -> "Заказ #${(orderState as UiState.Success<OrderModel>).data.id}"
                            else -> "Детали заказа"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = orderState) {
            is UiState.Loading ->
                LoadingView(message = "Загрузка заказа...")

            is UiState.Error ->
                ErrorView(message = state.message, onRetry = null)

            is UiState.Success ->
                OrderDetailContent(
                    order = state.data,
                    workerCount = workerCount,
                    isDispatcher = isDispatcher,
                    modifier = Modifier.padding(padding)
                )

            is UiState.Idle -> Unit
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderModel,
    workerCount: Int,
    isDispatcher: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Статус заказа
        StatusBadge(status = order.status)

        // Основная информация
        OrderInfoCard {
            InfoRow(Icons.Default.LocationOn,  "Адрес",        order.address)
            InfoRow(Icons.Default.Schedule,    "Дата и время", dateFormat.format(Date(order.dateTime)))
            InfoRow(Icons.Default.Inventory2,  "Груз",         order.cargoDescription)
        }

        // Условия
        OrderInfoCard {
            InfoRow(Icons.Default.Payments,    "Ставка",       "${order.pricePerHour.toInt()} ₽/час")
            InfoRow(Icons.Default.Timer,       "Длительность", "${order.estimatedHours} ч")
            InfoRow(Icons.Default.AttachMoney, "Итого",        "${order.totalPrice.toInt()} ₽")
        }

        // Грузчики
        OrderInfoCard {
            InfoRow(
                icon    = Icons.Default.Group,
                label   = "Грузчиков",
                value   = "$workerCount / ${order.requiredWorkers}"
            )
            if (order.minWorkerRating > 0f) {
                InfoRow(
                    icon  = Icons.Default.Star,
                    label = "Мин. рейтинг",
                    value = "%.1f ⭐".format(order.minWorkerRating)
                )
            }
        }

        // Комментарий
        if (order.comment.isNotBlank()) {
            OrderInfoCard {
                InfoRow(Icons.Default.Comment, "Комментарий", order.comment)
            }
        }

        // Дата завершения (если завершён)
        if (order.completedAt != null) {
            OrderInfoCard {
                InfoRow(
                    icon  = Icons.Default.CheckCircle,
                    label = "Завершён",
                    value = dateFormat.format(Date(order.completedAt))
                )
                if (order.workerRating != null) {
                    InfoRow(
                        icon  = Icons.Default.ThumbUp,
                        label = "Оценка",
                        value = "%.1f ⭐".format(order.workerRating)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: OrderStatusModel) {
    val (text, color) = when (status) {
        OrderStatusModel.AVAILABLE   -> "Доступен"   to MaterialTheme.colorScheme.primary
        OrderStatusModel.TAKEN       -> "Взят"        to MaterialTheme.colorScheme.tertiary
        OrderStatusModel.IN_PROGRESS -> "В работе"   to MaterialTheme.colorScheme.secondary
        OrderStatusModel.COMPLETED   -> "Завершён"   to MaterialTheme.colorScheme.secondary
        OrderStatusModel.CANCELLED   -> "Отменён"    to MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun OrderInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            content = content
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
