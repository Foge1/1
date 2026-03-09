package com.loaderapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.features.orders.presentation.OrdersTab
import com.loaderapp.ui.theme.LoaderAppTheme

private object StatsBarDefaults {
    val ContainerShape = AppShapes.medium
    val ItemShape = RoundedCornerShape(AppSpacing.sm + AppSpacing.xxs)
    val ContainerPadding = AppSpacing.xs
    val ItemVerticalPadding = AppSpacing.sm
    val ItemHorizontalPadding = AppSpacing.md
    val ItemSpacing = AppSpacing.xs
    const val ACTIVE_ITEM_ALPHA = 0.16f
    const val INACTIVE_ITEM_ALPHA = 0f
}

@Composable
fun StatsBar(
    counts: OrdersTabCounts,
    selected: OrdersTab,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = StatsBarDefaults.ContainerShape,
        color = AppColors.Surface,
        border = BorderStroke(width = AppSpacing.xxs, color = AppColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(StatsBarDefaults.ContainerPadding),
            horizontalArrangement = Arrangement.spacedBy(StatsBarDefaults.ItemSpacing),
        ) {
            StatsItem(
                label = OrdersTab.Available.title,
                value = counts.available,
                isActive = selected == OrdersTab.Available,
                modifier = Modifier.weight(1f),
            )
            StatsItem(
                label = OrdersTab.InProgress.title,
                value = counts.inProgress,
                isActive = selected == OrdersTab.InProgress,
                modifier = Modifier.weight(1f),
            )
            StatsItem(
                label = OrdersTab.History.title,
                value = counts.history,
                isActive = selected == OrdersTab.History,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsItem(
    label: String,
    value: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val itemBackgroundAlpha =
        if (isActive) {
            StatsBarDefaults.ACTIVE_ITEM_ALPHA
        } else {
            StatsBarDefaults.INACTIVE_ITEM_ALPHA
        }
    val labelColor = if (isActive) AppColors.Foreground else AppColors.MutedForeground

    Box(
        modifier =
            modifier
                .background(
                    color = AppColors.Primary.copy(alpha = itemBackgroundAlpha),
                    shape = StatsBarDefaults.ItemShape,
                ).padding(
                    horizontal = StatsBarDefaults.ItemHorizontalPadding,
                    vertical = StatsBarDefaults.ItemVerticalPadding,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (value > 999) "999+" else value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
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
            counts =
                OrdersTabCounts(
                    available = 14,
                    inProgress = 3,
                    history = 127,
                ),
            selected = OrdersTab.InProgress,
            modifier = Modifier.padding(AppSpacing.lg),
        )
    }
}
