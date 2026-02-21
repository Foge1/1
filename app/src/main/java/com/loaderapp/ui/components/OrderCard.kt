package com.loaderapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.theme.LoaderAppTheme
import com.loaderapp.ui.theme.statusAvailable
import com.loaderapp.ui.theme.statusCompleted
import com.loaderapp.ui.theme.statusInProgress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val canTakeOrder = enabled && order.status == OrderStatusModel.AVAILABLE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OrderCardHeader(order = order)
            OrderCardTitle(order = order)
            OrderMetaChips(order = order)
            if (order.comment.isNotBlank()) {
                OrderComment(comment = order.comment)
            }
            if (actionContent != null) {
                if (enabled) actionContent()
            } else {
                OrderCardActionButton(
                    order = order,
                    onClick = onClick,
                    enabled = canTakeOrder
                )
            }
        }
    }
}

@Composable
fun OrderCardHeader(order: OrderModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        OrderStatusChip(status = order.status)
        Text(
            text = "${order.pricePerHour.toInt()} ₽/ч",
            style = orderPriceTextStyle(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OrderCardTitle(order: OrderModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = order.address,
            style = orderTitleTextStyle(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatOrderDateTime(order.dateTime),
            style = orderDateTextStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderMetaChips(order: OrderModel, workerCount: Int? = null) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OrderMetaChip(icon = Icons.Rounded.Schedule, text = "${order.cargoDescription}")
        OrderMetaChip(icon = Icons.Rounded.Timelapse, text = "Мин ${order.estimatedHours}ч")
        val currentWorkers = workerCount ?: order.workerId?.let { 1 } ?: 0
        OrderMetaChip(icon = Icons.Rounded.Groups, text = "$currentWorkers/${order.requiredWorkers}")
    }
}

@Composable
private fun OrderMetaChip(icon: ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = orderMetaTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OrderComment(comment: String) {
    Text(
        text = comment,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun OrderCardActionButton(order: OrderModel, onClick: () -> Unit, enabled: Boolean) {
    val (title, colors) = when (order.status) {
        OrderStatusModel.AVAILABLE -> "Взять заказ" to ButtonDefaults.buttonColors()
        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS ->
            "Чат" to ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED ->
            "Завершён" to ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = colors
    ) {
        Text(text = title, style = orderActionTextStyle())
    }
}

@Composable
fun DispatcherOrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    OrderCard(
        order = order,
        onClick = onClick,
        modifier = modifier,
        actionContent = {
            if (order.status == OrderStatusModel.AVAILABLE || order.status == OrderStatusModel.TAKEN) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Отменить", style = orderActionTextStyle())
                }
            }
        }
    )

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отменить заказ?") },
            text = { Text("Вы уверены что хотите отменить этот заказ?") },
            confirmButton = {
                TextButton(
                    onClick = { onCancel(); showCancelDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Отменить заказ") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Назад") }
            }
        )
    }
}

@Composable
fun OrderStatusChip(status: OrderStatusModel) {
    val (text, containerColor, textColor) = when (status) {
        OrderStatusModel.AVAILABLE -> Triple("Доступен", statusAvailable.copy(alpha = 0.20f), Color(0xFF1B5E20))
        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> Triple("В работе", statusInProgress.copy(alpha = 0.20f), statusInProgress)
        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED ->
            Triple("Завершён", statusCompleted.copy(alpha = 0.20f), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}

@Composable
private fun orderPriceTextStyle(): TextStyle = MaterialTheme.typography.titleLarge.copy(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold
)

@Composable
private fun orderTitleTextStyle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold
)

@Composable
private fun orderDateTextStyle(): TextStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp)

@Composable
private fun orderMetaTextStyle(): TextStyle = MaterialTheme.typography.labelMedium.copy(
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium
)

@Composable
private fun orderActionTextStyle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold
)

fun formatOrderDateTime(timestamp: Long): String {
    val locale = Locale("ru")
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, target) -> "Сегодня ${SimpleDateFormat("HH:mm", locale).format(Date(timestamp))}"
        isSameDay(
            (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) },
            target
        ) -> "Завтра ${SimpleDateFormat("HH:mm", locale).format(Date(timestamp))}"

        isSameDay(
            (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) },
            target
        ) -> SimpleDateFormat("dd MMM HH:mm", locale).format(Date(timestamp))

        else -> SimpleDateFormat("dd.MM.yyyy HH:mm", locale).format(Date(timestamp))
    }
}

private fun isSameDay(first: Calendar, second: Calendar): Boolean =
    first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)

@Preview(showBackground = true)
@Composable
private fun OrderCardPreview() {
    LoaderAppTheme {
        OrderCard(
            order = OrderModel(
                id = 42,
                address = "Москва, ул. Тверская, 12",
                dateTime = System.currentTimeMillis(),
                cargoDescription = "Тал",
                pricePerHour = 750.0,
                estimatedHours = 4,
                requiredWorkers = 3,
                minWorkerRating = 4.5f,
                status = OrderStatusModel.AVAILABLE,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                workerId = null,
                dispatcherId = 1,
                workerRating = null,
                comment = "Позвонить за 15 минут"
            ),
            onClick = {}
        )
    }
}
