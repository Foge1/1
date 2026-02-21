package com.loaderapp.ui.loader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.loaderapp.R
import com.loaderapp.features.orders.ui.OrderUiModel
import com.loaderapp.features.orders.ui.OrdersTab
import com.loaderapp.features.orders.ui.OrdersViewModel
import com.loaderapp.features.orders.ui.toLegacyOrderModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.OrderCard
import com.loaderapp.ui.components.SwipeableTabs
import com.loaderapp.ui.components.TabItem
import com.loaderapp.ui.main.LocalBottomNavHeight

@Composable
fun LoaderScreen(
    viewModel: OrdersViewModel,
    onOrderClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    val tabs = listOf(
        TabItem(label = OrdersTab.Available.title, badgeCount = state.availableOrders.size),
        TabItem(label = OrdersTab.InProgress.title, badgeCount = state.inProgressOrders.size),
        TabItem(label = OrdersTab.History.title, badgeCount = state.historyOrders.size)
    )

    AppScaffold(title = "Заказы") {
        val topBarHeightPx = LocalTopBarHeightPx.current
        val density = LocalDensity.current
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        if (state.loading) {
            LoadingView()
        } else {
            SwipeableTabs(
                tabs = tabs,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeight + dimensionResource(id = R.dimen.order_spacing_8))
            ) { page ->
                when (page) {
                    0 -> OrdersPage(
                        orders = state.availableOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyTitle = "Нет доступных заказов",
                        emptyMessage = "Обновите страницу позже",
                        emptyIcon = Icons.Default.SearchOff,
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        action = { order ->
                            ActionButton("Взять заказ", pending = state.pendingActions.contains(order.order.id)) {
                                viewModel.acceptOrder(order.order.id)
                            }
                        }
                    )
                    1 -> OrdersPage(
                        orders = state.inProgressOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyTitle = "Нет заказов в работе",
                        emptyMessage = "Активные заказы появятся здесь",
                        emptyIcon = Icons.Default.WorkOff,
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        action = { order ->
                            ActionButton(
                                "Отменить",
                                pending = state.pendingActions.contains(order.order.id),
                                primary = false
                            ) { viewModel.cancelOrder(order.order.id) }
                        }
                    )
                    2 -> OrdersPage(
                        orders = state.historyOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyTitle = "История пуста",
                        emptyMessage = "Завершённые и отменённые заказы появятся здесь",
                        emptyIcon = Icons.Default.History,
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        action = { }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .padding(bottom = bottomNavHeight + dimensionResource(id = R.dimen.order_spacing_8))
        )
    }
}

@Composable
private fun OrdersPage(
    orders: List<OrderUiModel>,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
    emptyTitle: String,
    emptyMessage: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    pendingActions: Set<Long>,
    onOrderClick: (Long) -> Unit,
    action: @Composable (OrderUiModel) -> Unit
) {
    if (orders.isEmpty()) {
        EmptyStateView(icon = emptyIcon, title = emptyTitle, message = emptyMessage)
        return
    }

    FadingEdgeLazyColumn(
        modifier = Modifier.fillMaxSize(),
        topFadeHeight = 0.dp,
        bottomFadeHeight = 36.dp,
        contentPadding = PaddingValues(
            start = dimensionResource(id = R.dimen.order_spacing_16),
            end = dimensionResource(id = R.dimen.order_spacing_16),
            top = dimensionResource(id = R.dimen.order_spacing_8),
            bottom = bottomNavHeight + dimensionResource(id = R.dimen.order_spacing_24)
        )
    ) {
        items(orders, key = { it.order.id }) { order ->
            val legacy = order.toLegacyOrderModel()
            val pending = pendingActions.contains(order.order.id)
            OrderCard(
                order = legacy,
                onClick = { if (order.canOpenChat) onOrderClick(order.order.id) },
                enabled = !pending && (order.canOpenChat || order.canAccept),
                actionContent = { action(order) }
            )
            Spacer(Modifier.height(dimensionResource(id = R.dimen.order_spacing_12)))
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    pending: Boolean,
    primary: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !pending,
        modifier = Modifier.fillMaxWidth(),
        colors = if (primary) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Icon(Icons.Default.Check, null, Modifier.size(dimensionResource(id = R.dimen.order_spacing_16)))
        Spacer(Modifier.width(dimensionResource(id = R.dimen.order_spacing_8)))
        Text(if (pending) "Подождите..." else title)
    }
}
