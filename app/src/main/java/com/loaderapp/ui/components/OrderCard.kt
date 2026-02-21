package com.loaderapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.loaderapp.R
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.theme.LoaderAppTheme
import com.loaderapp.ui.theme.addressTextStyle
import com.loaderapp.ui.theme.dateTextStyle
import com.loaderapp.ui.theme.metaTextStyle
import com.loaderapp.ui.theme.rateTextStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null
) {
    val sectionSpacing = dimensionResource(id = R.dimen.order_card_section_spacing)
    val innerSpacing = dimensionResource(id = R.dimen.order_card_inner_spacing)
    val cardRadius = dimensionResource(id = R.dimen.order_card_corner_radius)
    val cardPadding = dimensionResource(id = R.dimen.order_card_padding)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = cardRadius / 8),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            HeaderRow(order = order)
            AddressDateBlock(order = order, innerSpacing = innerSpacing)
            MetaInfoRow(order = order, innerSpacing = innerSpacing)
            ActionButton(order = order, onClick = onClick, actionContent = actionContent)
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
        OrderStatusChip(status = order.status)
        Text(
            text = "${order.pricePerHour.toInt()} ₽/ч",
            style = rateTextStyle(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddressDateBlock(order: OrderModel, innerSpacing: androidx.compose.ui.unit.Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(innerSpacing / 2)) {
        Text(
            text = order.address,
            style = addressTextStyle(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatOrderDateTime(order.dateTime),
            style = dateTextStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetaInfoRow(order: OrderModel, innerSpacing: androidx.compose.ui.unit.Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetaItem(
            icon = Icons.Default.Person,
            value = "Нужно грузчиков: ${order.requiredWorkers}",
            innerSpacing = innerSpacing,
            modifier = Modifier.weight(1f)
        )
        MetaItem(
            icon = Icons.Default.Schedule,
            value = "Время работы: ${order.estimatedHours} ч",
            innerSpacing = innerSpacing,
            modifier = Modifier.weight(1f)
        )
        MetaItem(
            icon = Icons.Default.Star,
            value = "Требуемый рейтинг: ${order.minWorkerRating}",
            innerSpacing = innerSpacing,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetaItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    innerSpacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(innerSpacing / 2)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = R.dimen.order_icon_size)),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = metaTextStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(
    order: OrderModel,
    onClick: () -> Unit,
    actionContent: (@Composable () -> Unit)?
) {
    if (actionContent != null) {
        actionContent()
        return
    }

    val title = when (order.status) {
        OrderStatusModel.AVAILABLE -> "Взять заказ"
        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> "В работе"
        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED -> "Завершен"
    }
    Button(
        onClick = onClick,
        enabled = order.status == OrderStatusModel.AVAILABLE,
        modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth(),
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
        OrderStatusModel.AVAILABLE -> "Доступен" to Color(0xFF2E7D32)
        OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> "В работе" to Color(0xFF1565C0)
        OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED -> "Завершён" to Color(0xFF757575)
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
            style = metaTextStyle(),
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
        isSameDay(
            (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) },
            target
        ) -> SimpleDateFormat("dd MMM 'в' HH:mm", locale).format(Date(timestamp))
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
