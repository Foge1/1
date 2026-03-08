package com.loaderapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.ShapeButton
import com.loaderapp.core.ui.theme.ShapeCard
import com.loaderapp.core.ui.theme.ShapeChip
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.common.DateLabelFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OrderCardBorderWidth = 1.dp

@Composable
fun OrderCard(
    order: OrderModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val canTakeOrder = enabled && order.status == OrderStatusModel.AVAILABLE
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (enabled && isPressed) 0.97f else 1f,
            animationSpec = AppMotion.tweenMedium(),
            label = "order_card_scale",
        )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        shape = ShapeCard,
        color = AppColors.Surface,
        border = BorderStroke(width = OrderCardBorderWidth, color = AppColors.Border),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.xl)) {
            OrderCardHeader(order = order)
            OrderCardAddressRow(address = order.address)
            OrderCardDateTimeRow(order = order)
            OrderCardMetaRow(order = order)
            OrderCardCommentBlock(comment = order.comment)
            if (actionContent != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xl))
                actionContent()
            } else {
                OrderCTA(order = order, onClick = onClick, enabled = canTakeOrder)
            }
        }
    }
}

@Composable
private fun OrderCardHeader(order: OrderModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        StatusChip(status = order.status)
        OrderCardPrice(pricePerHour = order.pricePerHour)
    }
}

@Composable
private fun OrderCardPrice(pricePerHour: Double) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "₽${pricePerHour.toInt()}",
            style = orderPriceTextStyle(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "/ч",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = AppSpacing.xxs, bottom = AppSpacing.xxs),
        )
    }
}

@Composable
private fun OrderCardAddressRow(address: String) {
    Row(
        modifier = Modifier.padding(top = AppSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            modifier = Modifier.size(AppSpacing.lg),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = address,
            style = orderAddressTextStyle(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OrderCardDateTimeRow(order: OrderModel) {
    Row(
        modifier = Modifier.padding(top = AppSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(AppSpacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatOrderDate(order.dateTime),
                style = orderMetaTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.size(AppSpacing.xs),
            shape = ShapeChip,
            color = AppColors.MutedForeground,
        ) {}
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                modifier = Modifier.size(AppSpacing.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatOrderTime(order),
                style = orderMetaTextStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCardMetaRow(
    order: OrderModel,
    workerCount: Int? = null,
) {
    val currentWorkers = workerCount ?: order.workerId?.let { 1 } ?: 0
    FlowRow(
        modifier = Modifier.padding(top = AppSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        MetaChip(icon = Icons.Rounded.LocalShipping, text = order.cargoDescription)
        MetaChip(icon = Icons.Rounded.Timer, text = "${order.estimatedHours} ч")
        MetaChip(icon = Icons.Rounded.Groups, text = "$currentWorkers/${order.requiredWorkers}")
    }
}

@Composable
private fun OrderCardCommentBlock(comment: String) {
    if (comment.isBlank()) return

    Row(
        modifier =
            Modifier
                .padding(top = AppSpacing.lg)
                .background(
                    color = AppColors.Muted,
                    shape = ShapeChip,
                )
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Message,
            contentDescription = null,
            modifier = Modifier.size(AppSpacing.md),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = comment,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OrderCTA(
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
                .padding(top = AppSpacing.xl)
                .fillMaxWidth()
                .height(48.dp),
        shape = ShapeButton,
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
                    modifier = Modifier.padding(bottom = AppSpacing.sm),
                )
            }
            if (
                order.status == OrderStatusModel.AVAILABLE ||
                order.status == OrderStatusModel.TAKEN
            ) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = ShapeButton,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
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
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) { Text("Отменить заказ") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Назад") }
            },
        )
    }
}

@Composable
private fun orderPriceTextStyle(): TextStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)

@Composable
private fun orderAddressTextStyle(): TextStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

@Composable
private fun orderMetaTextStyle(): TextStyle = MaterialTheme.typography.bodyMedium

@Composable
private fun orderActionTextStyle(): TextStyle = MaterialTheme.typography.labelLarge

fun formatOrderDate(timestamp: Long): String = DateLabelFormatter.dateLabel(timestampMillis = timestamp)

fun formatOrderTime(order: OrderModel): String = if (order.isAsap) {
        "Ближайшее время"
    } else {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(order.dateTime))
    }

@Composable
fun OrderMetaChips(
    order: OrderModel,
    workerCount: Int? = null,
) {
    OrderCardMetaRow(
        order = order,
        workerCount = workerCount,
    )
}

@Composable
fun OrderComment(comment: String) = OrderCardCommentBlock(comment = comment)
