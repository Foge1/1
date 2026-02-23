package com.loaderapp.features.orders.ui

sealed class OrdersUiEvent {
    data class ShowSnackbar(val message: String) : OrdersUiEvent()
    data class NavigateToChat(val orderId: Long) : OrdersUiEvent()
    data class NavigateToDetails(val orderId: Long) : OrdersUiEvent()
}
