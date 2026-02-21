package com.loaderapp.features.orders.ui

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus

enum class OrdersTab(val title: String) {
    Available(title = "Доступны"),
    InProgress(title = "В работе"),
    History(title = "История")
}

fun OrdersTab.matches(status: OrderStatus): Boolean = when (this) {
    OrdersTab.Available -> status == OrderStatus.AVAILABLE
    OrdersTab.InProgress -> status == OrderStatus.IN_PROGRESS
    OrdersTab.History ->
        status == OrderStatus.COMPLETED ||
            status == OrderStatus.CANCELED ||
            status == OrderStatus.EXPIRED
}

fun OrdersTab.filter(orders: List<Order>): List<Order> = orders.filter { order -> matches(order.status) }

