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
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.common.DateLabelFormatter
import java.util.Date
import java.util.Locale

@Composable
fun OrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val canTakeOrder = enabled && order.status == OrderStatusModel.AVAILABLE

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OrderCardHeader(order = order)
            OrderCardTitle(order = order)
            OrderDateTimeRow(order = order)
            OrderMetaRow(order = order)
            OrderCommentBlock(comment = order.comment)
            if (actionContent != null) {
                Row(modifier = Modifier.padding(top = 20.dp)) {
                    actionContent()
                }
            } else {
                OrderCTA(order = order, onClick = onClick, enabled = canTakeOrder)
            }
        }
    }
}

@Composable
fun OrderCardHeader(order: OrderModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        StatusChip(status = order.status)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "₽${order.pricePerHour.toInt()}",
                style = orderPriceTextStyle(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "/ч",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
fun OrderCardTitle(order: OrderModel) {
    Text(
        text = order.address,
        style = orderTitleTextStyle(),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 14.dp),
    )
}

@Composable
fun OrderDateTimeRow(order: OrderModel) {
    Row(
        modifier = Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatOrderDate(order.dateTime),
                style = orderDateTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.size(2.dp),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {}
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatOrderTime(order),
                style = orderDateTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderMetaRow(
    order: OrderModel,
    workerCount: Int? = null,
) {
    val currentWorkers = workerCount ?: order.workerId?.let { 1 } ?: 0
    FlowRow(
        modifier = Modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaChip(icon = Icons.Rounded.LocalShipping, text = order.cargoDescription)
        MetaChip(icon = Icons.Rounded.Timer, text = "${order.estimatedHours}ч мин")
        MetaChip(icon = Icons.Rounded.Groups, text = "$currentWorkers/${order.requiredWorkers}")
    }
}

@Composable
fun OrderCommentBlock(comment: String) {
    if (comment.isBlank()) return

    Row(
        modifier =
            Modifier
                .padding(top = 14.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                ).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Message,
            contentDescription = null,
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = comment,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun OrderCTA(
    order: OrderModel,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val (title, colors) =
        when (order.status) {
            OrderStatusModel.AVAILABLE ->
                "Взять заказ" to
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    )

            OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS ->
                "Чат" to
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )

            OrderStatusModel.COMPLETED, OrderStatusModel.CANCELLED ->
                "Завершён" to
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    )
        }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = colors,
    ) {
        Text(text = title, style = orderActionTextStyle())
    }
}

@Composable
fun DispatcherOrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    showAcceptedBadge: Boolean = false,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    OrderCard(
        order = order,
        onClick = onClick,
        modifier = modifier,
        actionContent = {
            if (showAcceptedBadge) {
                Text(
                    text = "Взяли",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (order.status == OrderStatusModel.AVAILABLE || order.status == OrderStatusModel.TAKEN) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Отменить", style = orderActionTextStyle())
                }
            }
        },
    )

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отменить заказ?") },
            text = { Text("Вы уверены что хотите отменить этот заказ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Отменить заказ") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Назад") }
            },
        )
    }
}

@Composable
private fun orderPriceTextStyle(): TextStyle =
    MaterialTheme.typography.headlineSmall.copy(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    )

@Composable
private fun orderTitleTextStyle(): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
    )

@Composable
private fun orderDateTextStyle(): TextStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)

@Composable
private fun orderActionTextStyle(): TextStyle =
    MaterialTheme.typography.titleSmall.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    )

fun formatOrderDate(timestamp: Long): String = DateLabelFormatter.dateLabel(timestampMillis = timestamp)

fun formatOrderTime(order: OrderModel): String =
    if (order.isAsap) "Ближайшее время" else java.text.SimpleDateFormat("HH:mm", Locale("ru")).format(Date(order.dateTime))

@Composable
fun OrderMetaChips(
    order: OrderModel,
    workerCount: Int? = null,
) = OrderMetaRow(order, workerCount)

@Composable
fun OrderComment(comment: String) = OrderCommentBlock(comment)
