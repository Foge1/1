package com.loaderapp.features.orders.ui

import com.loaderapp.features.orders.domain.Order

sealed class OrdersCommand {
    data object Refresh : OrdersCommand()
    data class Create(val order: Order) : OrdersCommand()
    data class Accept(val orderId: Long) : OrdersCommand()
    data class Cancel(val orderId: Long, val reason: String?) : OrdersCommand()
    data class Complete(val orderId: Long) : OrdersCommand()
}
