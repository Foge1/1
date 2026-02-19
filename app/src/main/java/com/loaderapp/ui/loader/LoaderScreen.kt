package com.loaderapp.ui.loader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.OrderCard
import com.loaderapp.ui.components.SwipeableTabs
import com.loaderapp.ui.components.TabItem

/**
 * Экран грузчика.
 *
 * Структура:
 * - [GradientTopBar] «Заказы»
 * - Pill-табы «Доступные» / «Мои заказы» с поддержкой свайпа
 * - Первая вкладка: доступные для взятия заказы
 * - Вторая вкладка: взятые / в процессе заказы
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    onOrderClick: (Long) -> Unit
) {
    val availableState by viewModel.availableOrdersState.collectAsState()
    val myOrdersState  by viewModel.myOrdersState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val availableCount = (availableState as? UiState.Success)?.data?.size ?: 0
    val myOrdersCount  = (myOrdersState  as? UiState.Success)?.data?.size ?: 0

    val tabs = listOf(
        TabItem(label = "Доступные", badgeCount = availableCount),
        TabItem(label = "Мои заказы", badgeCount = myOrdersCount)
    )

    GradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar         = { GradientTopBar(title = "Заказы") },
            snackbarHost   = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->

            SwipeableTabs(
                tabs     = tabs,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 8.dp)
            ) { page ->
                when (page) {
                    0 -> AvailableOrdersPage(
                        state        = availableState,
                        onOrderClick = onOrderClick,
                        onTakeOrder  = { order ->
                            viewModel.takeOrder(order)
                        }
                    )
                    1 -> MyOrdersPage(
                        state           = myOrdersState,
                        onOrderClick    = onOrderClick,
                        onCompleteOrder = { order ->
                            viewModel.completeOrder(order)
                        }
                    )
                }
            }
        }
    }
}

/** Вкладка доступных заказов */
@Composable
private fun AvailableOrdersPage(
    state: UiState<List<OrderModel>>,
    onOrderClick: (Long) -> Unit,
    onTakeOrder: (OrderModel) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Error   -> ErrorView(message = state.message)
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyStateView(
                    icon    = Icons.Default.SearchOff,
                    title   = "Нет доступных заказов",
                    message = "Обновите страницу позже"
                )
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { order ->
                        OrderCard(
                            order   = order,
                            onClick = { onOrderClick(order.id) },
                            actionContent = {
                                Button(
                                    onClick  = { onTakeOrder(order) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Взять заказ")
                                }
                            }
                        )
                    }
                }
            }
        }
        is UiState.Idle -> EmptyStateView(
            icon    = Icons.Default.Search,
            title   = "Поиск заказов",
            message = "Загрузка..."
        )
    }
}

/** Вкладка «Мои заказы» */
@Composable
private fun MyOrdersPage(
    state: UiState<List<OrderModel>>,
    onOrderClick: (Long) -> Unit,
    onCompleteOrder: (OrderModel) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Error   -> ErrorView(message = state.message)
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyStateView(
                    icon    = Icons.Default.WorkOff,
                    title   = "Нет активных заказов",
                    message = "Возьмите заказ из вкладки «Доступные»"
                )
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { order ->
                        OrderCard(
                            order   = order,
                            onClick = { onOrderClick(order.id) },
                            actionContent = if (order.status == OrderStatusModel.TAKEN ||
                                               order.status == OrderStatusModel.IN_PROGRESS) {
                                {
                                    Button(
                                        onClick  = { onCompleteOrder(order) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Завершить")
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
        is UiState.Idle -> EmptyStateView(
            icon    = Icons.Default.Work,
            title   = "Мои заказы",
            message = "Здесь будут ваши активные заказы"
        )
    }
}
