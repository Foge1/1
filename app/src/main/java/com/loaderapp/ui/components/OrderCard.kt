package com.loaderapp.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.WatchLater
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.theme.LoaderAppTheme
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
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OrderCardHeader(order = order)
            OrderCardTitle(order = order, modifier = Modifier.padding(top = 14.dp))
            OrderDateTimeRow(order = order, modifier = Modifier.padding(top = 10.dp))
            OrderMetaRow(order = order, modifier = Modifier.padding(top = 16.dp))
            OrderCommentBlock(comment = order.comment, modifier = Modifier.padding(top = 14.dp))

            if (actionContent != null) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    if (enabled) actionContent()
                }
            } else {
                OrderCTA(
                    order = order,
                    onClick = onClick,
                    enabled = canTakeOrder,
                    modifier = Modifier.padding(top = 20.dp)
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
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "₽${order.pricePerHour.toInt()}",
                style = orderPriceTextStyle(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/ч",
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderCardTitle(order: OrderModel, modifier: Modifier = Modifier) {
    Text(
        text = order.address,
        modifier = modifier,
        style = orderTitleTextStyle(),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun OrderDateTimeRow(order: OrderModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatOrderDate(order.dateTime),
                style = orderDateTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            modifier = Modifier.size(4.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ) {}
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Rounded.WatchLater,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatOrderTime(order.dateTime),
                style = orderDateTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderMetaRow(order: OrderModel, modifier: Modifier = Modifier, workerCount: Int? = null) {
    val currentWorkers = workerCount ?: order.workerId?.let { 1 } ?: 0

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OrderMetaChip(icon = Icons.Rounded.Schedule, text = order.cargoDescription)
        OrderMetaChip(icon = Icons.Rounded.Timelapse, text = "${order.estimatedHours}ч мин")
        OrderMetaChip(icon = Icons.Rounded.Groups, text = "$currentWorkers/${order.requiredWorkers}")
    }
}

@Composable
fun OrderCommentBlock(comment: String, modifier: Modifier = Modifier) {
    if (comment.isBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.Message,
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = comment,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OrderCTA(order: OrderModel, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val (title, colors) = when (order.status) {
        OrderStatusModel.AVAILABLE -> "Взять заказ" to ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface
        )

        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS ->
            "Чат" to ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED ->
            "Завершён" to ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = colors
    ) {
        Text(text = title, style = orderActionTextStyle())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderMetaChips(order: OrderModel, workerCount: Int? = null) {
    OrderMetaRow(order = order, workerCount = workerCount)
}

@Composable
private fun OrderMetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
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
    OrderCommentBlock(comment = comment)
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
                    shape = RoundedCornerShape(12.dp),
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
        OrderStatusModel.AVAILABLE -> Triple(
            "Доступен",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> Triple(
            "В работе",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED -> Triple(
            "Завершён",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                fontSize = 12.sp
            ),
            color = textColor
        )
    }
}

@Composable
private fun orderPriceTextStyle(): TextStyle = MaterialTheme.typography.headlineSmall.copy(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.3).sp
)

@Composable
private fun orderTitleTextStyle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 22.sp
)

@Composable
private fun orderDateTextStyle(): TextStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)

@Composable
private fun orderMetaTextStyle(): TextStyle = MaterialTheme.typography.labelSmall.copy(
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium
)

@Composable
private fun orderActionTextStyle(): TextStyle = MaterialTheme.typography.titleSmall.copy(
    fontSize = 14.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.3.sp
)

private fun formatOrderDate(timestamp: Long): String {
    return SimpleDateFormat("dd MMM", Locale("ru")).format(Date(timestamp))
}

private fun formatOrderTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale("ru")).format(Date(timestamp))
}

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
