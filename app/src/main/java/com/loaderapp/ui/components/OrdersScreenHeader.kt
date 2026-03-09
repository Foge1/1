package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography

private object OrdersScreenHeaderDefaults {
    val HorizontalPadding = AppSpacing.lg
    val HeaderTopPadding = AppSpacing.sm
    val TitleSubtitleSpacing = AppSpacing.xxs
    val RoleLabelTopSpacing = AppSpacing.xs
    val StatsCardPadding = AppSpacing.sm
    val StatsCardSpacing = AppSpacing.xs
    val StatsIconSize = AppSpacing.lg
    val StatsValueTopSpacing = AppSpacing.xs
    val StatsLabelTopSpacing = AppSpacing.xxs
}

enum class OrdersScreenRole(
    val title: String,
) {
    Dispatcher("Диспетчер"),
    Loader("Грузчик"),
}

data class OrdersSummaryUi(
    val available: Int,
    val inProgress: Int,
    val history: Int,
    val responses: Int,
)

@Composable
fun OrdersScreenHeader(
    title: String,
    subtitle: String?,
    role: OrdersScreenRole,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = OrdersScreenHeaderDefaults.HorizontalPadding)
                .padding(top = OrdersScreenHeaderDefaults.HeaderTopPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.headlineSmall,
                color = AppColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(OrdersScreenHeaderDefaults.TitleSubtitleSpacing))
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = AppColors.MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(OrdersScreenHeaderDefaults.RoleLabelTopSpacing))
            Text(
                text = role.title,
                style = AppTypography.labelMedium,
                color = AppColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Поиск",
                tint = AppColors.Foreground,
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Уведомления",
                tint = AppColors.Foreground,
            )
        }
    }
}

@Composable
fun OrdersStatsSummary(
    summary: OrdersSummaryUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = OrdersScreenHeaderDefaults.HorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(OrdersScreenHeaderDefaults.StatsCardSpacing),
    ) {
        SummaryCard(
            icon = Icons.Outlined.Assignment,
            label = "Доступно",
            value = summary.available,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            icon = Icons.Outlined.PlayCircleOutline,
            label = "В работе",
            value = summary.inProgress,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            icon = Icons.Outlined.History,
            label = "История",
            value = summary.history,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            icon = Icons.Outlined.Group,
            label = "Отклики",
            value = summary.responses,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = AppColors.Surface,
        shape = AppShapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(OrdersScreenHeaderDefaults.StatsCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.MutedForeground,
                modifier = Modifier.size(OrdersScreenHeaderDefaults.StatsIconSize),
            )
            Spacer(modifier = Modifier.height(OrdersScreenHeaderDefaults.StatsValueTopSpacing))
            Text(
                text = value.toString(),
                style = AppTypography.titleMedium,
                color = AppColors.Foreground,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(OrdersScreenHeaderDefaults.StatsLabelTopSpacing))
            Text(
                text = label,
                style = AppTypography.labelSmall,
                color = AppColors.MutedForeground,
                maxLines = 1,
            )
        }
    }
}
