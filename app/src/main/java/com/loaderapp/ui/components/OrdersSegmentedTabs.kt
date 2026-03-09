package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.features.orders.presentation.OrdersTab

private object OrdersSegmentedTabDefaults {
    val StatsToTabsSpacing = AppSpacing.sm
    val TabsToPagerSpacing = AppSpacing.xs
    val TrackHorizontalPadding = AppSpacing.lg
    val StatsHorizontalPadding = AppSpacing.lg
}

data class OrdersTabCounts(
    val available: Int,
    val inProgress: Int,
    val history: Int,
)

@Composable
fun OrdersSegmentedTabs(
    selected: OrdersTab,
    onSelect: (OrdersTab) -> Unit,
    modifier: Modifier = Modifier,
    counts: OrdersTabCounts? = null,
    content: @Composable (pageIndex: Int) -> Unit,
) {
    val tabs =
        listOf(
            TabItem(label = OrdersTab.Available.title, badgeCount = counts?.available ?: 0),
            TabItem(label = OrdersTab.InProgress.title, badgeCount = counts?.inProgress ?: 0),
            TabItem(label = OrdersTab.History.title, badgeCount = counts?.history ?: 0),
        )

    Column(modifier = modifier.fillMaxSize()) {
        if (counts != null) {
            StatsBar(
                counts = counts,
                selected = selected,
                modifier = Modifier.padding(horizontal = OrdersSegmentedTabDefaults.StatsHorizontalPadding),
            )
            Spacer(modifier = Modifier.height(OrdersSegmentedTabDefaults.StatsToTabsSpacing))
        }

        SwipeableTabs(
            tabs = tabs,
            modifier = Modifier.fillMaxSize(),
            initialPage = selected.ordinal,
            onPageChanged = { onSelect(OrdersTab.entries[it]) },
            tabsToPagerSpacing = OrdersSegmentedTabDefaults.TabsToPagerSpacing,
            tabRowHorizontalPadding = OrdersSegmentedTabDefaults.TrackHorizontalPadding,
            content = content,
        )
    }
}
