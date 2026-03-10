package com.loaderapp.ui.components

import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.presentation.OrdersUiState

private const val EMPTY_INCOME_VALUE = "—"

fun OrdersUiState.toStatsBarUiModel(): StatsBarUiModel {
    val completedOrdersCount = historyOrders.count { it.order.status == OrderStatus.COMPLETED }
    val canceledOrdersCount = historyOrders.count { it.order.status == OrderStatus.CANCELED }

    return StatsBarUiModel(
        active = (availableOrders.size + inProgressOrders.size).toString(),
        completed = completedOrdersCount.toString(),
        canceled = canceledOrdersCount.toString(),
        income = EMPTY_INCOME_VALUE,
    )
}
