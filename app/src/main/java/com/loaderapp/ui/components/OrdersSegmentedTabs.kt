package com.loaderapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.features.orders.presentation.OrdersTab

private object OrdersSegmentedTabDefaults {
    val TabsToPagerSpacing = 4.dp
    val TabVerticalPadding = 6.dp
    val TabHorizontalPadding = 6.dp
    val TrackHorizontalPadding = 16.dp
}

data class OrdersTabCounts(
    val available: Int,
    val inProgress: Int,
    val history: Int
)

@Composable
fun OrdersSegmentedTabs(
    selected: OrdersTab,
    onSelect: (OrdersTab) -> Unit,
    modifier: Modifier = Modifier,
    counts: OrdersTabCounts? = null,
    content: @Composable (pageIndex: Int) -> Unit,
) {
    val tabs = listOf(
        TabItem(label = OrdersTab.Available.title, badgeCount = counts?.available ?: 0),
        TabItem(label = OrdersTab.InProgress.title, badgeCount = counts?.inProgress ?: 0),
        TabItem(label = OrdersTab.History.title, badgeCount = 0)
    )

    SwipeableTabs(
        tabs = tabs,
        modifier = modifier,
        initialPage = selected.ordinal,
        onPageChanged = { onSelect(OrdersTab.entries[it]) },
        tabsToPagerSpacing = OrdersSegmentedTabDefaults.TabsToPagerSpacing,
        tabVerticalPadding = OrdersSegmentedTabDefaults.TabVerticalPadding,
        tabHorizontalPadding = OrdersSegmentedTabDefaults.TabHorizontalPadding,
        tabRowHorizontalPadding = OrdersSegmentedTabDefaults.TrackHorizontalPadding,
        content = content
    )
}
