package com.loaderapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.ui.theme.LoaderAppTheme

private object StatsBarDefaults {
    val CardShape = AppShapes.small
    val CardPadding = AppSpacing.sm
    val CardSpacing = AppSpacing.xs
    val CardContentSpacing = AppSpacing.xxs
    val IconSize = AppSpacing.lg
    val BorderWidth = 1.dp
}

data class StatsBarUiModel(
    val active: String,
    val completed: String,
    val canceled: String,
    val income: String,
)

@Composable
fun StatsBar(
    stats: StatsBarUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StatsBarDefaults.CardSpacing),
    ) {
        StatsCard(
            icon = Icons.Outlined.PlayCircleOutline,
            iconTint = AppColors.StatusInProgressFg,
            value = stats.active,
            label = "Активные",
            modifier = Modifier.weight(1f),
        )
        StatsCard(
            icon = Icons.Outlined.TaskAlt,
            iconTint = AppColors.StatusCompletedFg,
            value = stats.completed,
            label = "Завершено",
            modifier = Modifier.weight(1f),
        )
        StatsCard(
            icon = Icons.Outlined.Cancel,
            iconTint = AppColors.StatusCanceledFg,
            value = stats.canceled,
            label = "Отменено",
            modifier = Modifier.weight(1f),
        )
        StatsCard(
            icon = Icons.Outlined.AccountBalanceWallet,
            iconTint = AppColors.Accent,
            value = stats.income,
            label = "Доход",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatsCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = StatsBarDefaults.CardShape,
        color = AppColors.Surface,
        border = BorderStroke(width = StatsBarDefaults.BorderWidth, color = AppColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(StatsBarDefaults.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StatsBarDefaults.CardContentSpacing),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(StatsBarDefaults.IconSize),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsBarPreview() {
    LoaderAppTheme {
        StatsBar(
            stats =
                StatsBarUiModel(
                    active = "14",
                    completed = "9",
                    canceled = "2",
                    income = "—",
                ),
            modifier = Modifier.padding(AppSpacing.lg),
        )
    }
}
