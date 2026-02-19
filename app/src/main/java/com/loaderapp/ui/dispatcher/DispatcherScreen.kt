package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.ui.components.DispatcherOrderCard
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.SwipeableTabs
import com.loaderapp.ui.components.TabItem
import kotlinx.coroutines.FlowPreview

/**
 * Экран диспетчера.
 *
 * Структура:
 * - [GradientTopBar] с именем диспетчера
 * - Pill-табы «Свободные» / «В работе» с поддержкой свайпа
 * - [SwipeableTabs] → [HorizontalPager] между страницами
 * - FAB «Создать заказ»
 *
 * Фильтрация по статусу выполняется здесь, бизнес-логика — в ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun DispatcherScreen(
    viewModel: DispatcherViewModel,
    onOrderClick: (Long) -> Unit
) {
    val ordersState       by viewModel.ordersState.collectAsState()
    val snackbarHostState  = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Разбиваем заказы по статусам для табов
    val allOrders   = (ordersState as? UiState.Success)?.data ?: emptyList()
    val freeOrders  = allOrders.filter { it.status == OrderStatusModel.AVAILABLE }
    val activeOrders = allOrders.filter {
        it.status == OrderStatusModel.TAKEN || it.status == OrderStatusModel.IN_PROGRESS
    }

    val tabs = listOf(
        TabItem(label = "Свободные", badgeCount = freeOrders.size),
        TabItem(label = "В работе",  badgeCount = activeOrders.size)
    )

    GradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = { GradientTopBar(title = "Диспетчер") },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick        = { showCreateDialog = true },
                    icon           = { Icon(Icons.Default.Add, "Создать заказ") },
                    text           = { Text("Создать заказ") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->

            when (ordersState) {
                is UiState.Loading -> LoadingView()
                is UiState.Error   -> ErrorView(message = (ordersState as UiState.Error).message)
                else -> {
                    // SwipeableTabs занимает всё пространство под TopBar
                    SwipeableTabs(
                        tabs     = tabs,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(top = 8.dp)   // небольшой отступ под TopBar
                    ) { page ->
                        when (page) {
                            0 -> OrdersPage(
                                orders      = freeOrders,
                                emptyIcon   = Icons.Default.Assignment,
                                emptyTitle  = "Нет свободных заказов",
                                emptyMsg    = "Создайте первый заказ нажав на кнопку +",
                                onOrderClick = onOrderClick,
                                onCancelOrder = { viewModel.cancelOrder(it) }
                            )
                            1 -> OrdersPage(
                                orders      = activeOrders,
                                emptyIcon   = Icons.Default.WorkOff,
                                emptyTitle  = "Нет активных заказов",
                                emptyMsg    = "Взятые грузчиками заказы появятся здесь",
                                onOrderClick = onOrderClick,
                                onCancelOrder = { viewModel.cancelOrder(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate  = { order ->
                viewModel.createOrder(order) { showCreateDialog = false }
            }
        )
    }
}

/** Страница списка заказов внутри таба */
@Composable
private fun OrdersPage(
    orders: List<OrderModel>,
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
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                DispatcherOrderCard(
                    order   = order,
                    onClick = { onOrderClick(order.id) },
                    onCancel = { onCancelOrder(order) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
