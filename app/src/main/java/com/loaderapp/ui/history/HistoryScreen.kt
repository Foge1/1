package com.loaderapp.ui.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.core.ui.components.input.AppSearchField
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.presentation.history.HistoryViewModel
import com.loaderapp.ui.common.asString
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.EmptyStateView
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.FadingEdgeLazyColumn
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.OrderCard
import com.loaderapp.ui.components.staggeredItemAppearance
import com.loaderapp.ui.main.LocalBottomNavHeight

@Composable
fun HistoryScreen(
    userId: Long,
    userRole: UserRoleModel,
    onOrderClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    LaunchedEffect(userId, userRole) { viewModel.initialize(userId, userRole) }

    val historyState by viewModel.historyState.collectAsState()
    val query by viewModel.query.collectAsState()

    AppScaffold(title = "История") {
        val topBarHeightPx = LocalTopBarHeightPx.current
        val density = LocalDensity.current
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        HistoryScreenContent(
            state = historyState,
            query = query,
            topBarHeight = topBarHeight,
            bottomNavHeight = bottomNavHeight,
            onQueryChanged = viewModel::onQueryChanged,
            onOrderClick = onOrderClick,
        )
    }
}

@Composable
private fun HistoryScreenContent(
    state: UiState<List<OrderModel>>,
    query: String,
    topBarHeight: Dp,
    bottomNavHeight: Dp,
    onQueryChanged: (String) -> Unit,
    onOrderClick: (Long) -> Unit,
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(message = state.message.asString())
        is UiState.Success -> {
            FadingEdgeLazyColumn(
                modifier = Modifier.fillMaxSize(),
                bottomFadeHeight = 36.dp,
                contentPadding =
                    PaddingValues(
                        start = AppSpacing.lg,
                        end = AppSpacing.lg,
                        top = topBarHeight + AppSpacing.sm,
                        bottom = bottomNavHeight + 48.dp,
                    ),
            ) {
                historySearchSection(
                    query = query,
                    onQueryChanged = onQueryChanged,
                )
                historyResultsSection(
                    orders = state.data,
                    query = query,
                    onOrderClick = onOrderClick,
                )
            }
        }
        is UiState.Idle -> Unit
    }
}

private fun LazyListScope.historySearchSection(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    item {
        AppSearchField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = "Поиск по истории",
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
    }
}

private fun LazyListScope.historyResultsSection(
    orders: List<OrderModel>,
    query: String,
    onOrderClick: (Long) -> Unit,
) {
    if (orders.isEmpty()) {
        item {
            EmptyStateView(
                icon = Icons.Default.History,
                title = if (query.isBlank()) "История пуста" else "Ничего не найдено",
                message =
                    if (query.isBlank()) {
                        "Завершённые и отменённые заказы появятся здесь"
                    } else {
                        "Попробуйте изменить поисковый запрос"
                    },
            )
        }
    } else {
        itemsIndexed(items = orders, key = { _, item -> item.id }) { index, order ->
            OrderCard(
                order = order,
                onClick = { onOrderClick(order.id) },
                modifier = Modifier.staggeredItemAppearance(index = index),
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
