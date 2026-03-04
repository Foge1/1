package com.loaderapp.features.orders.presentation

data class OrderDetailUiState(
    val loading: Boolean = true,
    val requiresUserSelection: Boolean = false,
    val order: OrderUiModel? = null,
    val errorMessage: String? = null,
    val isActionInProgress: Boolean = false,
)
