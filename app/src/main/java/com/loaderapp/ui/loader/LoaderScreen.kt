package com.loaderapp.ui.loader

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.R
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.presentation.DispatcherHistoryUiState
import com.loaderapp.features.orders.presentation.OrderUiModel
import com.loaderapp.features.orders.presentation.OrdersTab
import com.loaderapp.features.orders.presentation.OrdersViewModel
import com.loaderapp.features.orders.presentation.mapper.toLegacyOrderModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import com.loaderapp.ui.components.HistoryScreen
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.OrdersSegmentedTabs
import com.loaderapp.ui.components.OrdersHeaderDimens
import com.loaderapp.ui.components.OrdersTabCounts
import com.loaderapp.ui.components.OrderCard
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

    var selectedTab by rememberSaveable { mutableStateOf(OrdersTab.Available) }

    AppScaffold(title = "Заказы") {
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
                    0 -> OrdersListPage(
                        orders = state.availableOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyTitle = "Нет доступных заказов",
                        emptyMessage = "Обновите страницу позже",
                        emptyIcon = Icons.Default.SearchOff,
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        actionSlot = { order ->
                            // Всё решение принимается на основе order.myApplicationStatus
                            // (статус заявки текущего грузчика) и computed-флагов из StateMachine.
                            LoaderAvailableActions(
                                order = order,
                                pending = state.pendingActions.contains(order.order.id),
                                onApply = { viewModel.onApplyClicked(order.order.id) },
                                onWithdraw = { viewModel.onWithdrawClicked(order.order.id) }
                            )
                        }
                    )
                    1 -> OrdersListPage(
                        orders = state.inProgressOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyTitle = "Нет заказов в работе",
                        emptyMessage = "Активные заказы появятся здесь",
                        emptyIcon = Icons.Default.WorkOff,
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        actionSlot = { order ->
                            // canCancel уже вычислен StateMachine — грузчик может отменять
                            // только если у него есть ACTIVE assignment (по доменным правилам).
                            if (order.canCancel) {
                                SecondaryOutlinedButton(
                                    label = "Отменить",
                                    pending = state.pendingActions.contains(order.order.id),
                                    onClick = { viewModel.onCancelClicked(order.order.id) }
                                )
                            }
                        }
                    )
                    2 -> LoaderHistoryPage(
                        historyState = state.history,
                        onHistoryQueryChanged = viewModel::onHistoryQueryChanged,
                        bottomNavHeight = bottomNavHeight,
                        onOrderClick = onOrderClick
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
private fun LoaderHistoryPage(
    historyState: DispatcherHistoryUiState,
    onHistoryQueryChanged: (String) -> Unit,
    bottomNavHeight: Dp,
    onOrderClick: (Long) -> Unit,
) {
    HistoryScreen(
        state = historyState,
        onQueryChange = onHistoryQueryChanged,
        onOrderClick = onOrderClick,
        bottomPadding = bottomNavHeight + 72.dp,
    )
}

// ── Available-tab action area ─────────────────────────────────────────────────

/**
 * CTA для карточки в статусе STAFFING (вкладка "Доступны").
 *
 * Три состояния, определяемые [OrderUiModel.myApplicationStatus]:
 * - null / WITHDRAWN / REJECTED → кнопка "Отозваться" (enabled/disabled + reason)
 * - APPLIED → кнопка "Отозвать отклик"
 * - SELECTED → бейдж "Вы отобраны" + опциональная кнопка "Отозвать"
 *
 * Логика прав — только через [OrderUiModel]; никаких status-проверок здесь.
 */
@Composable
private fun LoaderAvailableActions(
    order: OrderUiModel,
    pending: Boolean,
    onApply: () -> Unit,
    onWithdraw: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (order.myApplicationStatus) {
            OrderApplicationStatus.SELECTED -> {
                SelectedBadge()
                if (order.canWithdraw) {
                    Spacer(Modifier.height(8.dp))
                    WithdrawButton(
                        pending = pending,
                        disabledReason = null, // canWithdraw == true → кнопка активна
                        onClick = onWithdraw
                    )
                }
            }

            OrderApplicationStatus.APPLIED -> {
                WithdrawButton(
                    pending = pending,
                    disabledReason = if (!order.canWithdraw) order.withdrawDisabledReason else null,
                    onClick = onWithdraw
                )
            }

            // null, WITHDRAWN, REJECTED — показываем "Отозваться"
            else -> {
                ApplyButton(
                    pending = pending,
                    enabled = order.canApply,
                    disabledReason = order.applyDisabledReason,
                    onClick = onApply
                )
            }
        }
    }
}

// ── Reusable button atoms ─────────────────────────────────────────────────────

@Composable
private fun ApplyButton(
    pending: Boolean,
    enabled: Boolean,
    disabledReason: String?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            enabled = enabled && !pending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (pending) "Подождите..." else "Отозваться")
        }

        // Причина блокировки видна только когда кнопка disabled и нет pending-операции.
        if (!enabled && !pending && disabledReason != null) {
            Spacer(Modifier.height(6.dp))
            DisabledReasonHint(disabledReason)
        }
    }
}

@Composable
private fun WithdrawButton(
    pending: Boolean,
    disabledReason: String?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onClick,
            enabled = disabledReason == null && !pending,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Cancel,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (pending) "Подождите..." else "Отозвать отклик")
        }

        if (disabledReason != null && !pending) {
            Spacer(Modifier.height(6.dp))
            DisabledReasonHint(disabledReason)
        }
    }
}

@Composable
private fun SelectedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Вы отобраны",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DisabledReasonHint(reason: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = reason,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun SecondaryOutlinedButton(
    label: String,
    pending: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !pending,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(if (pending) "Подождите..." else label)
    }
}

// ── Shared list scaffold ──────────────────────────────────────────────────────

@Composable
private fun OrdersListPage(
    orders: List<OrderUiModel>,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
    emptyTitle: String,
    emptyMessage: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
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
            start = dimensionResource(id = R.dimen.order_spacing_16),
            end = dimensionResource(id = R.dimen.order_spacing_16),
            top = dimensionResource(id = R.dimen.order_spacing_8),
            bottom = bottomNavHeight + dimensionResource(id = R.dimen.order_spacing_24)
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
            Spacer(Modifier.height(dimensionResource(id = R.dimen.order_spacing_12)))
        }
    }
}
