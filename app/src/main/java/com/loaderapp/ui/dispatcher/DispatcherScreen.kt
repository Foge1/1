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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
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

    val tabs = listOf(
        TabItem(label = OrdersTab.Available.title, badgeCount = state.availableOrders.size),
        TabItem(label = OrdersTab.InProgress.title, badgeCount = state.inProgressOrders.size),
        TabItem(label = OrdersTab.History.title, badgeCount = 0)
    )

    AppScaffold(title = "Диспетчер") {
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
                    .padding(top = topBarHeight + 8.dp)
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
                        state = state.history,
                        bottomNavHeight = bottomNavHeight,
                        onOrderClick = onOrderClick,
                        onQueryChange = viewModel::onHistoryQueryChanged
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
    state: com.loaderapp.features.orders.ui.DispatcherHistoryUiState,
    bottomNavHeight: Dp,
    onOrderClick: (Long) -> Unit,
    onQueryChange: (String) -> Unit
) {
    var localQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(state.query))
    }

    LaunchedEffect(state.query) {
        if (state.query.isEmpty() && localQuery.text.isNotEmpty()) {
            localQuery = TextFieldValue("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = localQuery,
            onValueChange = { newValue ->
                localQuery = newValue
                onQueryChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text("Поиск по истории") }
        )

        if (state.sections.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.History,
                title = "История пуста",
                message = "Нет заказов, подходящих под фильтр"
            )
            return
        }

        FadingEdgeLazyColumn(
            modifier = Modifier.fillMaxSize(),
            topFadeHeight = 0.dp,
            bottomFadeHeight = 36.dp,
            contentPadding = PaddingValues(top = 12.dp, bottom = bottomNavHeight + 80.dp)
        ) {
            itemsIndexed(state.sections, key = { _, section -> section.key }) { index, section ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleSmall)
                    Text(section.count.toString(), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                section.items.forEach { item ->
                    OrderCard(
                        order = item.order.toLegacyOrderModel(),
                        onClick = { onOrderClick(item.order.order.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (index < state.sections.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
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
