package com.loaderapp.ui.loader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.OrderCard
import com.loaderapp.ui.components.SwipeableTabs
import com.loaderapp.ui.components.TabItem
import com.loaderapp.ui.main.LocalBottomNavHeight

/**
 * Экран грузчика.
 *
 * TopBar-высота читается из [LocalTopBarHeightPx] — точное значение
 * в px, измеренное SubcomposeLayout в [AppScaffold]. Конвертируется
 * в dp через [LocalDensity] для использования в padding/fade.
 *
 * Нижний contentPadding = [LocalBottomNavHeight] + запас для fade,
 * чтобы последняя карточка была полностью доступна при скролле.
 */
@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    onOrderClick: (Long) -> Unit
) {
    val availableState by viewModel.availableOrdersState.collectAsState()
    val myOrdersState  by viewModel.myOrdersState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    val availableCount = (availableState as? UiState.Success)?.data?.size ?: 0
    val myOrdersCount  = (myOrdersState  as? UiState.Success)?.data?.size ?: 0

    val tabs = listOf(
        TabItem(label = "Доступные", badgeCount = availableCount),
        TabItem(label = "Мои заказы", badgeCount = myOrdersCount)
    )

    AppScaffold(title = "Заказы") {
        // Точная высота TopBar из SubcomposeLayout — без хардкода
        val topBarHeightPx = LocalTopBarHeightPx.current
        val density        = LocalDensity.current
        val topBarHeight   = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        SwipeableTabs(
            tabs     = tabs,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight + 8.dp)
        ) { page ->
            when (page) {
                0 -> AvailableOrdersPage(
                    state           = availableState,
                    bottomNavHeight = bottomNavHeight,
                    onOrderClick    = onOrderClick,
                    onTakeOrder     = { viewModel.takeOrder(it) }
                )
                1 -> MyOrdersPage(
                    state           = myOrdersState,
                    bottomNavHeight = bottomNavHeight,
                    onOrderClick    = onOrderClick,
                    onCompleteOrder = { viewModel.completeOrder(it) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = bottomNavHeight + 8.dp)
        )
    }
}

@Composable
private fun AvailableOrdersPage(
    state: UiState<List<OrderModel>>,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
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
                        Spacer(Modifier.height(12.dp))
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

@Composable
private fun MyOrdersPage(
    state: UiState<List<OrderModel>>,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
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
                        Spacer(Modifier.height(12.dp))
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
