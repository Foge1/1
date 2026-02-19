package com.loaderapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Универсальная карточка заказа.
 *
 * [actionContent] — слот для кнопок действий, специфичных для роли:
 *   - Dispatcher: кнопка "Отменить заказ"
 *   - Loader: кнопки "Взять" / "Завершить"
 *   - null: только просмотр
 */
@Composable
fun OrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок + статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                OrderStatusChip(status = order.status)
            }

            // Описание
            Text(
                text = order.cargoDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Дата
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday, null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatOrderDateTime(order.dateTime),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Параметры
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OrderParamChip(Icons.Default.Person,      "${order.requiredWorkers} чел")
                OrderParamChip(Icons.Default.Schedule,    "${order.estimatedHours} ч")
                OrderParamChip(Icons.Default.AttachMoney, "${order.pricePerHour}₽/ч")
                OrderParamChip(Icons.Default.Star,        "≥${order.minWorkerRating}")
            }

            // Слот для кнопок роли
            actionContent?.invoke()
        }
    }
}

/**
 * Карточка с диалогом отмены — для экрана Диспетчера.
 */
@Composable
fun DispatcherOrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    OrderCard(
        order    = order,
        onClick  = onClick,
        modifier = modifier,
        actionContent = {
            if (order.status == OrderStatusModel.AVAILABLE || order.status == OrderStatusModel.TAKEN) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Отменить заказ")
                }
            }
        }
    )

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title   = { Text("Отменить заказ?") },
            text    = { Text("Вы уверены что хотите отменить этот заказ?") },
            confirmButton = {
                TextButton(
                    onClick = { onCancel(); showCancelDialog = false },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Отменить заказ") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Назад") }
            }
        )
    }
}

// ── Shared UI ────────────────────────────────────────────────────────────────

@Composable
fun OrderStatusChip(status: OrderStatusModel) {
    val (text, color) = when (status) {
        OrderStatusModel.AVAILABLE   -> "Доступен"  to Color(0xFF4CAF50)
        OrderStatusModel.TAKEN       -> "Взят"      to Color(0xFFFF9800)
        OrderStatusModel.IN_PROGRESS -> "В работе"  to Color(0xFF2196F3)
        OrderStatusModel.COMPLETED   -> "Завершён"  to Color(0xFF9C27B0)
        OrderStatusModel.CANCELLED   -> "Отменён"   to Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OrderParamChip(icon: ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatOrderDateTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(timestamp))
