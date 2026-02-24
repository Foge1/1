package com.loaderapp.features.orders.ui

data class OrdersUiState(
    val availableOrders: List<OrderUiModel> = emptyList(),
    val inProgressOrders: List<OrderUiModel> = emptyList(),
    val historyOrders: List<OrderUiModel> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val pendingActions: Set<Long> = emptySet(),
    val requiresUserSelection: Boolean = false,
    val responsesBadgeCount: Int = 0,
    val history: DispatcherHistoryUiState = DispatcherHistoryUiState()
)
