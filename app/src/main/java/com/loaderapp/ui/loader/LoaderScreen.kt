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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Work
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

@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    onOrderClick: (Long) -> Unit
) {
    val availableState by viewModel.availableOrdersState.collectAsState()
    val myOrdersState  by viewModel.myOrdersState.collectAsState()
    val workerRating by viewModel.workerRating.collectAsState()
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
        val topBarHeightPx = LocalTopBarHeightPx.current
        val density        = LocalDensity.current
        val topBarHeight   = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        SwipeableTabs(
            tabs     = tabs,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight + dimensionResource(id = R.dimen.order_spacing_8))
        ) { page ->
            when (page) {
                0 -> AvailableOrdersPage(
                    state = availableState,
                    workerRating = workerRating,
                    bottomNavHeight = bottomNavHeight,
                    onOrderClick = onOrderClick,
                    onTakeOrder = { viewModel.takeOrder(it) }
                )
                1 -> MyOrdersPage(
                    state = myOrdersState,
                    bottomNavHeight = bottomNavHeight,
                    onOrderClick = onOrderClick,
                    onCompleteOrder = { viewModel.completeOrder(it) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .padding(bottom = bottomNavHeight + dimensionResource(id = R.dimen.order_spacing_8))
        )
    }
}

@Composable
private fun AvailableOrdersPage(
    state: UiState<List<OrderModel>>,
    workerRating: Float,
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
                    icon = Icons.Default.SearchOff,
                    title = "Нет доступных заказов",
                    message = "Обновите страницу позже"
                )
            } else {
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
                    items(state.data, key = { it.id }) { order ->
                        val canTakeOrder = workerRating >= order.minWorkerRating
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) },
                            enabled = canTakeOrder,
                            actionContent = if (canTakeOrder) {
                                {
                                    Button(
                                        onClick  = { onTakeOrder(order) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, null, Modifier.size(dimensionResource(id = R.dimen.order_spacing_16)))
                                        Spacer(Modifier.width(dimensionResource(id = R.dimen.order_spacing_8)))
                                        Text("Взять заказ")
                                    }
                                }
                            } else {
                                null
                            }
                        )
                        Spacer(Modifier.height(dimensionResource(id = R.dimen.order_spacing_12)))
                    }
                }
            }
        }
        is UiState.Idle -> EmptyStateView(
            icon = Icons.Default.Search,
            title = "Поиск заказов",
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
                    icon = Icons.Default.WorkOff,
                    title = "Нет активных заказов",
                    message = "Возьмите заказ из вкладки «Доступные»"
                )
            } else {
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
                    items(state.data, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) },
                            actionContent = if (order.status == OrderStatusModel.TAKEN ||
                                order.status == OrderStatusModel.IN_PROGRESS
                            ) {
                                {
                                    Button(
                                        onClick = { onCompleteOrder(order) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, null, Modifier.size(dimensionResource(id = R.dimen.order_spacing_16)))
                                        Spacer(Modifier.width(dimensionResource(id = R.dimen.order_spacing_8)))
                                        Text("Завершить")
                                    }
                                }
                            } else null
                        )
                        Spacer(Modifier.height(dimensionResource(id = R.dimen.order_spacing_12)))
                    }
                }
            }
        }
        is UiState.Idle -> EmptyStateView(
            icon = Icons.Default.Work,
            title = "Мои заказы",
            message = "Здесь будут ваши активные заказы"
        )
    }
}
