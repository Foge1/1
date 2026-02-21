package com.loaderapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.R
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.theme.LoaderAppTheme
import com.loaderapp.ui.theme.orderBodyStyle
import com.loaderapp.ui.theme.orderLabelStyle
import com.loaderapp.ui.theme.orderTitleLargeStyle
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
    val sectionSpacing = 12.dp
    val innerSpacing = 8.dp
    val cardRadius = 20.dp
    val cardPadding = dimensionResource(id = R.dimen.order_card_padding)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            HeaderRow(order = order)
            ParamsBlock(order = order, innerSpacing = innerSpacing)
            CommentBlock(comment = order.comment)
            ActionButton(order = order, onClick = onClick, actionContent = actionContent, enabled = enabled)
        }
    }
}

@Composable
private fun HeaderRow(order: OrderModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = order.address,
                style = orderTitleLargeStyle().copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatOrderDateTime(order.dateTime),
                style = orderLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${order.pricePerHour.toInt()} ₽/ч",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            OrderStatusChip(status = order.status)
        }
    }
}

@Composable
private fun ParamsBlock(order: OrderModel, innerSpacing: Dp) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(innerSpacing)
    ) {
        ParamRow(
            icon = Icons.Rounded.Inventory2,
            title = "Тип груза",
            value = order.cargoDescription,
            innerSpacing = innerSpacing,
            labelWidth = 84.dp
        )
        ParamRow(
            icon = Icons.Rounded.Schedule,
            title = "Минимум",
            value = "${order.estimatedHours} ч",
            innerSpacing = innerSpacing,
            labelWidth = 84.dp
        )
        ParamRow(
            icon = Icons.Rounded.Groups,
            title = "Состав",
            value = "1 / ${order.requiredWorkers} грузчиков",
            innerSpacing = innerSpacing,
            labelWidth = 84.dp
        )
    }
}

@Composable
private fun ParamRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    innerSpacing: Dp,
    labelWidth: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(innerSpacing / 2)
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size)),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = orderBodyStyle().copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "—",
            style = orderBodyStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = orderBodyStyle(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CommentBlock(comment: String) {
    if (comment.isBlank()) return

    Text(
        text = comment,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ActionButton(
    order: OrderModel,
    onClick: () -> Unit,
    actionContent: (@Composable () -> Unit)?,
    enabled: Boolean
) {
    if (actionContent != null) {
        if (enabled) {
            actionContent()
        }
        return
    }

    val title = when (order.status) {
        OrderStatusModel.AVAILABLE -> "Взять заказ"
        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> "В работе"
        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED -> "Завершен"
    }
    Button(
        onClick = onClick,
        enabled = enabled && order.status == OrderStatusModel.AVAILABLE,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(title)
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
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Отменить заказ")
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
    val (text, color) = when (status) {
        OrderStatusModel.AVAILABLE -> "Доступен" to statusAvailable
        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> "В работе" to statusInProgress
        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED -> "Завершён" to statusCompleted
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = dimensionResource(id = R.dimen.order_status_horizontal_padding),
                vertical = dimensionResource(id = R.dimen.order_status_vertical_padding)
            ),
            style = orderLabelStyle().copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

fun formatOrderDateTime(timestamp: Long): String {
    val locale = Locale("ru")
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, target) -> "Сегодня в ${SimpleDateFormat("HH:mm", locale).format(Date(timestamp))}"
        isSameDay(
            (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) },
            target
        ) -> "Завтра в ${SimpleDateFormat("HH:mm", locale).format(Date(timestamp))}"
        else -> SimpleDateFormat("dd.MM.yyyy", locale).format(Date(timestamp))
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
                cargoDescription = "Коробки",
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
