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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import com.loaderapp.core.ui.components.button.AppDangerButton
import com.loaderapp.core.ui.components.button.AppPrimaryButton
import com.loaderapp.core.ui.components.button.AppSecondaryButton
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.ui.common.DateLabelFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OrderCardBorderWidth = AppSpacing.xxs / 2
private val OrderCardActionHeight = AppSpacing.xxxl + AppSpacing.lg

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
                }.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        shape = AppShapes.medium,
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
            style = MaterialTheme.typography.headlineSmall,
            color = AppColors.Foreground,
        )
        Text(
            text = "/ч",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.MutedForeground,
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
            tint = AppColors.MutedForeground,
        )
        Text(
            text = address,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Foreground,
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
                tint = AppColors.MutedForeground,
            )
            Text(
                text = formatOrderDate(order.dateTime),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.MutedForeground,
            )
        }
        Surface(
            modifier = Modifier.size(AppSpacing.xs),
            shape = AppShapes.extraSmall,
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
                tint = AppColors.MutedForeground,
            )
            Text(
                text = formatOrderTime(order),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.MutedForeground,
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
        MetaChip(icon = Icons.Rounded.Timer, text = formatOrderDuration(order.estimatedHours))
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
                    shape = AppShapes.extraSmall,
                ).padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Message,
            contentDescription = null,
            modifier = Modifier.size(AppSpacing.md),
            tint = AppColors.MutedForeground,
        )
        Text(
            text = comment,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.MutedForeground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OrderCTA(
    order: OrderModel,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    when (order.status) {
        OrderStatusModel.AVAILABLE -> {
            AppPrimaryButton(
                text = "Взять заказ",
                onClick = onClick,
                enabled = enabled,
                modifier =
                    Modifier
                        .padding(top = AppSpacing.xl)
                        .fillMaxWidth()
                        .height(OrderCardActionHeight),
            )
        }

        OrderStatusModel.TAKEN,
        OrderStatusModel.IN_PROGRESS,
        -> {
            AppSecondaryButton(
                text = "Чат",
                onClick = onClick,
                enabled = enabled,
                modifier =
                    Modifier
                        .padding(top = AppSpacing.xl)
                        .fillMaxWidth()
                        .height(OrderCardActionHeight),
            )
        }

        OrderStatusModel.COMPLETED,
        OrderStatusModel.CANCELLED,
        -> {
            AppPrimaryButton(
                text = "Завершён",
                onClick = onClick,
                enabled = enabled,
                modifier =
                    Modifier
                        .padding(top = AppSpacing.xl)
                        .fillMaxWidth()
                        .height(OrderCardActionHeight),
            )
        }
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
                    color = AppColors.Accent,
                    modifier = Modifier.padding(bottom = AppSpacing.sm),
                )
            }
            if (
                order.status == OrderStatusModel.AVAILABLE ||
                order.status == OrderStatusModel.TAKEN
            ) {
                AppDangerButton(
                    text = "Отменить",
                    onClick = { showCancelDialog = true },
                    enabled = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(OrderCardActionHeight),
                )
            }
        },
    )

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Отменить заказ?") },
            text = { Text("Вы уверены что хотите отменить этот заказ?") },
            confirmButton = {
                AppDangerButton(
                    text = "Отменить заказ",
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    },
                )
            },
            dismissButton = {
                AppSecondaryButton(
                    text = "Назад",
                    onClick = { showCancelDialog = false },
                )
            },
        )
    }
}

fun formatOrderDate(timestamp: Long): String = DateLabelFormatter.dateLabel(timestampMillis = timestamp)

fun formatOrderTime(order: OrderModel): String =
    if (order.isAsap) {
        "Ближайшее время"
    } else {
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(order.dateTime))
    }

fun formatOrderDuration(estimatedHours: Int): String = "от ${estimatedHours.coerceAtLeast(1)} ч"

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
