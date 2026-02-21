package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.ui.OrdersTab
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.DispatcherOrderCard
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.SwipeableTabs
import com.loaderapp.ui.components.TabItem
import com.loaderapp.ui.main.LocalBottomNavHeight
import kotlinx.coroutines.FlowPreview

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun DispatcherScreen(
    viewModel: DispatcherViewModel,
    onOrderClick: (Long) -> Unit,
    onNavigateToCreateOrder: () -> Unit
) {
    val ordersState       by viewModel.ordersState.collectAsState()
    val snackbarHostState  = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    val allOrders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val freeOrders = allOrders.filterByTab(OrdersTab.Available)
    val activeOrders = allOrders.filterByTab(OrdersTab.InProgress)
    val historyOrders = allOrders.filterByTab(OrdersTab.History)

    val tabs = listOf(
        TabItem(label = OrdersTab.Available.title, badgeCount = freeOrders.size),
        TabItem(label = OrdersTab.InProgress.title, badgeCount = activeOrders.size),
        TabItem(label = OrdersTab.History.title, badgeCount = historyOrders.size)
    )

    AppScaffold(title = "Диспетчер") {
        val topBarHeightPx  = LocalTopBarHeightPx.current
        val density         = LocalDensity.current
        val topBarHeight    = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        when (ordersState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error   -> ErrorView(message = (ordersState as UiState.Error).message)
            else -> {
                SwipeableTabs(
                    tabs     = tabs,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeight + 8.dp)
                ) { page ->
                    when (page) {
                        0 -> OrdersPage(
                            orders          = freeOrders,
                            bottomNavHeight = bottomNavHeight,
                            emptyIcon       = Icons.Default.Assignment,
                            emptyTitle      = "Нет доступных заказов",
                            emptyMsg        = "Создайте первый заказ, нажав на кнопку +",
                            onOrderClick    = onOrderClick,
                            onCancelOrder   = { viewModel.cancelOrder(it) }
                        )
                        1 -> OrdersPage(
                            orders          = activeOrders,
                            bottomNavHeight = bottomNavHeight,
                            emptyIcon       = Icons.Default.WorkOff,
                            emptyTitle      = "Нет заказов в работе",
                            emptyMsg        = "Активные заказы появятся здесь",
                            onOrderClick    = onOrderClick,
                            onCancelOrder   = { viewModel.cancelOrder(it) }
                        )
                        2 -> OrdersPage(
                            orders          = historyOrders,
                            bottomNavHeight = bottomNavHeight,
                            emptyIcon       = Icons.Default.History,
                            emptyTitle      = "История пуста",
                            emptyMsg        = "Завершённые и отменённые заказы появятся здесь",
                            onOrderClick    = onOrderClick,
                            onCancelOrder   = { viewModel.cancelOrder(it) }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = bottomNavHeight + 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                onClick        = onNavigateToCreateOrder,
                icon           = { Icon(Icons.Default.Add, "Создать заказ") },
                text           = { Text("Создать заказ") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomNavHeight + 8.dp)
        )
    }
}

@Composable
private fun OrdersPage(
    orders: List<OrderModel>,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyTitle: String,
    emptyMsg: String,
    onOrderClick: (Long) -> Unit,
    onCancelOrder: (OrderModel) -> Unit
) {
    if (orders.isEmpty()) {
        EmptyStateView(icon = emptyIcon, title = emptyTitle, message = emptyMsg)
    } else {
        FadingEdgeLazyColumn(
            modifier         = Modifier.fillMaxSize(),
            topFadeHeight    = 0.dp,
            bottomFadeHeight = 36.dp,
            contentPadding   = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = 8.dp,
                bottom = bottomNavHeight + 48.dp
            )
        ) {
            items(orders, key = { it.id }) { order ->
                DispatcherOrderCard(
                    order    = order,
                    onClick  = { onOrderClick(order.id) },
                    onCancel = { onCancelOrder(order) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}


private fun List<OrderModel>.filterByTab(tab: OrdersTab): List<OrderModel> =
    filter { order -> tab.matches(order.toFeatureStatus()) }

private fun OrderModel.toFeatureStatus() = when (status) {
    OrderStatusModel.AVAILABLE -> com.loaderapp.features.orders.domain.OrderStatus.AVAILABLE
    OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> com.loaderapp.features.orders.domain.OrderStatus.IN_PROGRESS
    OrderStatusModel.COMPLETED -> com.loaderapp.features.orders.domain.OrderStatus.COMPLETED
    OrderStatusModel.CANCELLED -> com.loaderapp.features.orders.domain.OrderStatus.CANCELED
}
