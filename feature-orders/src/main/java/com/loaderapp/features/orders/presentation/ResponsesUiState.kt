package com.loaderapp.features.orders.presentation

data class ResponsesUiState(
    val items: List<OrderResponsesUiModel> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val pendingActions: Set<Long> = emptySet(),
)

data class OrderResponsesUiModel(
    val orderId: Long,
    val address: String,
    val cargoText: String,
    val requiredCount: Int,
    val selectedCount: Int,
    val responsesCount: Int,
    val responses: List<ResponseRowUiModel>,
    val canStart: Boolean,
    val startDisabledReason: String?,
)

data class ResponseRowUiModel(
    val loaderId: String,
    val loaderName: String,
    val isSelected: Boolean,
    val canToggle: Boolean,
    val toggleDisabledReason: String?,
    val isBusy: Boolean,
    val busyOrderId: Long?,
)
