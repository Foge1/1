package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.features.orders.presentation.OrderUiModel
import com.loaderapp.features.orders.presentation.DispatcherHistoryUiState
import com.loaderapp.features.orders.presentation.OrdersTab
import com.loaderapp.features.orders.presentation.OrdersViewModel
import com.loaderapp.features.orders.presentation.mapper.toLegacyOrderModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.OrderCard
import com.loaderapp.ui.components.HistoryScreen
import com.loaderapp.ui.components.OrdersHeaderDimens
import com.loaderapp.ui.components.OrdersSegmentedTabs
import com.loaderapp.ui.components.OrdersTabCounts
import com.loaderapp.ui.main.LocalBottomNavHeight

/**
 * Экран диспетчера. Использует [OrdersViewModel] и [OrderUiModel] как единственный
 * источник флагов доступности действий — никаких проверок статуса в composable.
 */
@Composable
fun DispatcherScreen(
    viewModel: OrdersViewModel,
    onOrderClick: (Long) -> Unit,
    onNavigateToCreateOrder: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    var selectedTab by rememberSaveable { mutableStateOf(OrdersTab.Available) }

    AppScaffold(title = "Диспетчер") {
        val topBarHeightPx = LocalTopBarHeightPx.current
        val density = LocalDensity.current
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        if (state.loading) {
            LoadingView()
        } else {
            OrdersSegmentedTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                counts = OrdersTabCounts(
                    available = state.availableOrders.size,
                    inProgress = state.inProgressOrders.size,
                    history = state.historyOrders.size
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeight + OrdersHeaderDimens.tabsTopPadding)
            ) { page ->
                when (page) {
                    0 -> DispatcherOrdersPage(
                        orders = state.availableOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyIcon = Icons.Default.Assignment,
                        emptyTitle = "Нет доступных заказов",
                        emptyMessage = "Создайте первый заказ, нажав +",
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        actionSlot = { order ->
                            StaffingOrderActions(
                                order = order,
                                pending = state.pendingActions.contains(order.order.id),
                                onCancel = { viewModel.onCancelClicked(order.order.id) }
                            )
                        }
                    )
                    1 -> DispatcherOrdersPage(
                        orders = state.inProgressOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyIcon = Icons.Default.WorkOff,
                        emptyTitle = "Нет заказов в работе",
                        emptyMessage = "Активные заказы появятся здесь",
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        actionSlot = { order ->
                            InProgressOrderActions(
                                order = order,
                                pending = state.pendingActions.contains(order.order.id),
                                onCancel = { viewModel.onCancelClicked(order.order.id) }
                            )
                        }
                    )
                    2 -> DispatcherHistoryPage(
                        historyState = state.history,
                        onHistoryQueryChanged = viewModel::onHistoryQueryChanged,
                        bottomNavHeight = bottomNavHeight,
                        onOrderClick = onOrderClick
                    )
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
                onClick = onNavigateToCreateOrder,
                icon = { Icon(Icons.Default.Add, contentDescription = "Создать заказ") },
                text = { Text("Создать заказ") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomNavHeight + 8.dp)
        )
    }
}

// ── Action blocks ─────────────────────────────────────────────────────────────

/** Блок действий для заказа в STAFFING: статус откликов + отмена заказа. */
@Composable
private fun StaffingOrderActions(
    order: OrderUiModel,
    pending: Boolean,
    onCancel: () -> Unit,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Отклики: ${order.visibleApplicants.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (order.canCancel) {
            Spacer(Modifier.height(8.dp))
            CancelOutlinedButton(pending = pending, onClick = { showCancelDialog = true })
        }
    }

    if (showCancelDialog) {
        CancelConfirmDialog(
            onConfirm = { onCancel(); showCancelDialog = false },
            onDismiss = { showCancelDialog = false }
        )
    }
}

/** Блок действий для заказа в IN_PROGRESS: только "Отменить". */
@Composable
private fun InProgressOrderActions(
    order: OrderUiModel,
    pending: Boolean,
    onCancel: () -> Unit,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (order.canCancel) {
        CancelOutlinedButton(pending = pending, onClick = { showCancelDialog = true })
    }

    if (showCancelDialog) {
        CancelConfirmDialog(
            onConfirm = { onCancel(); showCancelDialog = false },
            onDismiss = { showCancelDialog = false }
        )
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun CancelOutlinedButton(pending: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !pending,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.Cancel,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Отменить заказ")
    }
}

@Composable
private fun CancelConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отменить заказ?") },
        text = { Text("Вы уверены, что хотите отменить этот заказ?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Да, отменить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Назад") }
        }
    )
}

@Composable
private fun DispatcherHistoryPage(
    historyState: DispatcherHistoryUiState,
    onHistoryQueryChanged: (String) -> Unit,
    bottomNavHeight: Dp,
    onOrderClick: (Long) -> Unit,
) {
    HistoryScreen(
        state = historyState,
        onQueryChange = onHistoryQueryChanged,
        onOrderClick = onOrderClick,
        bottomPadding = bottomNavHeight + 80.dp
    )
}

// ── Orders list scaffold ──────────────────────────────────────────────────────

@Composable
private fun DispatcherOrdersPage(
    orders: List<OrderUiModel>,
    bottomNavHeight: Dp,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    pendingActions: Set<Long>,
    onOrderClick: (Long) -> Unit,
    actionSlot: @Composable (OrderUiModel) -> Unit,
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
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = bottomNavHeight + 80.dp // extra space for FAB
        )
    ) {
        items(orders, key = { it.order.id }) { order ->
            val pending = pendingActions.contains(order.order.id)
            OrderCard(
                order = order.toLegacyOrderModel(),
                onClick = { onOrderClick(order.order.id) },
                enabled = !pending,
                actionContent = { actionSlot(order) }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
