package com.loaderapp.ui.main

import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.presentation.OrdersUiState
import com.loaderapp.presentation.dispatcher.DispatcherStatsUiState
import com.loaderapp.ui.components.StatsBarUiModel

private const val EMPTY_INCOME_VALUE = "—"

data class DispatcherStatsBarState(
    val active: Int,
    val completed: Int,
    val canceled: Int,
    val income: String?,
)

fun dispatcherStatsBarState(
    statsState: DispatcherStatsUiState,
    ordersState: OrdersUiState?,
): DispatcherStatsBarState {
    val canceledCount = ordersState?.historyOrders?.count { it.order.status == OrderStatus.CANCELED } ?: 0
    return DispatcherStatsBarState(
        active = statsState.active,
        completed = statsState.completed,
        canceled = canceledCount,
        income = null,
    )
}

fun DispatcherStatsBarState.toStatsBarUiModel(): StatsBarUiModel =
    StatsBarUiModel(
        active = active.toString(),
        completed = completed.toString(),
        canceled = canceled.toString(),
        income = income ?: EMPTY_INCOME_VALUE,
    )
