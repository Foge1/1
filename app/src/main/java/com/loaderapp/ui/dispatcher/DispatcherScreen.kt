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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        TabItem(label = OrdersTab.History.title, badgeCount = state.historyOrders.size)
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
                                onStart = { viewModel.onStartClicked(order.order.id) },
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
                    2 -> DispatcherOrdersPage(
                        orders = state.historyOrders,
                        bottomNavHeight = bottomNavHeight,
                        emptyIcon = Icons.Default.History,
                        emptyTitle = "История пуста",
                        emptyMessage = "Завершённые и отменённые заказы появятся здесь",
                        pendingActions = state.pendingActions,
                        onOrderClick = onOrderClick,
                        actionSlot = { }
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

/**
 * Блок действий для заказа в STAFFING:
 * - список откликов (только APPLIED + SELECTED) с кнопками select/unselect
 * - кнопка "Запустить" — видна только при [OrderUiModel.canStart] или при наличии прав
 * - кнопка "Отменить"
 *
 * Все флаги берутся из [OrderUiModel]; composable не проверяет статус заказа напрямую.
 */
@Composable
private fun StaffingOrderActions(
    order: OrderUiModel,
    pending: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Отклики: ${order.visibleApplicants.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (order.canSelect || order.canStart) {
            Spacer(Modifier.height(12.dp))
            StartButton(
                canStart = order.canStart,
                pending = pending,
                disabledReason = order.startDisabledReason,
                onClick = onStart
            )
        }

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

// ── Applicants block ──────────────────────────────────────────────────────────

/**
 * Карточка с откликами.
 *
 * Показывает только APPLIED + SELECTED (фильтрация через [OrderUiModel.visibleApplicants]).
 * Статус APPLIED → кнопка "+" (select), SELECTED → кнопка со значком Check (unselect).
 */
@Composable
private fun ApplicantsBlock(
    applicants: List<OrderApplication>,
    selectedCount: Int,
    workersTotal: Int,
    canSelect: Boolean,
    canUnselect: Boolean,
    actionsBlocked: Boolean,
    onSelect: (String) -> Unit,
    onUnselect: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок: "Отклики: N  Выбрано: M/K"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Отклики: ${applicants.size}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val countColor = if (selectedCount == workersTotal) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = "Выбрано: $selectedCount/$workersTotal",
                    style = MaterialTheme.typography.labelMedium,
                    color = countColor
                )
            }

            if (applicants.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Откликов пока нет",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                applicants.forEach { application ->
                    ApplicantRow(
                        application = application,
                        canSelect = canSelect && !actionsBlocked,
                        canUnselect = canUnselect && !actionsBlocked,
                        onSelect = { onSelect(application.loaderId) },
                        onUnselect = { onUnselect(application.loaderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicantRow(
    application: OrderApplication,
    canSelect: Boolean,
    canUnselect: Boolean,
    onSelect: () -> Unit,
    onUnselect: () -> Unit,
) {
    val isSelected = application.status == OrderApplicationStatus.SELECTED
    val appliedAt = remember(application.appliedAtMillis) {
        SimpleDateFormat("dd MMM HH:mm", Locale("ru")).format(Date(application.appliedAtMillis))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column {
                Text(
                    text = application.loaderId,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                // Место для ratingSnapshot — покажем когда появится в модели.
                Text(
                    text = appliedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            isSelected && canUnselect -> {
                // SELECTED + права снятия → заполненная кнопка (нажать = unselect)
                FilledTonalIconButton(
                    onClick = onUnselect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Снять выбор",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            isSelected -> {
                // SELECTED, но прав нет → просто бейдж
                SelectedChip()
            }
            canSelect -> {
                // APPLIED + права выбора → кнопка "+"
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Выбрать",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // APPLIED, прав нет → ничего не показываем
        }
    }
}

@Composable
private fun SelectedChip() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "Выбран",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun StartButton(
    canStart: Boolean,
    pending: Boolean,
    disabledReason: String?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            enabled = canStart && !pending,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(if (pending) "Запуск..." else "Запустить заказ")
        }

        if (!canStart && disabledReason != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = disabledReason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
