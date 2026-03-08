package com.loaderapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.ShapeStatusPill
import com.loaderapp.domain.model.OrderStatusModel

@Composable
fun StatusChip(
    status: OrderStatusModel,
    modifier: Modifier = Modifier,
) {
    val chipState = status.toStatusChipUiState()
    val colorAnimationSpec =
        tween<Color>(
            durationMillis = AppMotion.DURATION_MEDIUM,
            easing = AppMotion.EASING_STANDARD,
        )
    val containerColor =
        animateColorAsState(
            targetValue = chipState.containerColor,
            animationSpec = colorAnimationSpec,
            label = "status_chip_container",
        )
    val contentColor =
        animateColorAsState(
            targetValue = chipState.contentColor,
            animationSpec = colorAnimationSpec,
            label = "status_chip_content",
        )

    Surface(
        modifier = modifier.semantics { contentDescription = "Статус: ${chipState.label}" },
        color = containerColor.value,
        shape = ShapeStatusPill,
    ) {
        Text(
            text = chipState.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.value,
            modifier =
                Modifier.padding(
                    horizontal = AppSpacing.sm,
                    vertical = AppSpacing.xs,
                ),
        )
    }
}

private data class StatusChipUiState(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

private fun OrderStatusModel.toStatusChipUiState(): StatusChipUiState =
    when (this) {
        OrderStatusModel.AVAILABLE ->
            StatusChipUiState(
                label = "Новый",
                containerColor = AppColors.StatusStaffingBg,
                contentColor = AppColors.StatusStaffingFg,
            )

        OrderStatusModel.TAKEN ->
            StatusChipUiState(
                label = "Принят",
                containerColor = AppColors.StatusInProgressBg,
                contentColor = AppColors.StatusInProgressFg,
            )

        OrderStatusModel.IN_PROGRESS ->
            StatusChipUiState(
                label = "В работе",
                containerColor = AppColors.StatusInProgressBg,
                contentColor = AppColors.StatusInProgressFg,
            )

        OrderStatusModel.COMPLETED ->
            StatusChipUiState(
                label = "Завершён",
                containerColor = AppColors.StatusCompletedBg,
                contentColor = AppColors.StatusCompletedFg,
            )

        OrderStatusModel.CANCELLED ->
            StatusChipUiState(
                label = "Отменён",
                containerColor = AppColors.StatusCanceledBg,
                contentColor = AppColors.StatusCanceledFg,
            )
    }
