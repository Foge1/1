package com.loaderapp.features.orders.ui

data class DispatcherHistoryUiState(
    val query: String = "",
    val sections: List<HistorySectionUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HistorySectionUi(
    val key: String,
    val title: String,
    val count: Int,
    val items: List<OrderHistoryItemUi>
)

data class OrderHistoryItemUi(
    val order: OrderUiModel
)
