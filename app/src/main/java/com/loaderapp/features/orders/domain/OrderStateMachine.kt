package com.loaderapp.features.orders.domain

object OrderStateMachine {
    data class OrderActions(
        val canAccept: Boolean,
        val canCancel: Boolean,
        val canComplete: Boolean,
        val canOpenChat: Boolean
    )

    fun actionsFor(order: Order): OrderActions {
        return OrderActions(
            canAccept = order.status == OrderStatus.AVAILABLE,
            canCancel = order.status == OrderStatus.AVAILABLE || order.status == OrderStatus.IN_PROGRESS,
            canComplete = order.status == OrderStatus.IN_PROGRESS,
            canOpenChat = order.status == OrderStatus.IN_PROGRESS
        )
    }

    fun canTransition(from: OrderStatus, to: OrderStatus): Boolean {
        return when (from) {
            OrderStatus.AVAILABLE -> to in setOf(OrderStatus.IN_PROGRESS, OrderStatus.CANCELED, OrderStatus.EXPIRED)
            OrderStatus.IN_PROGRESS -> to in setOf(OrderStatus.COMPLETED, OrderStatus.CANCELED)
            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> false
        }
    }

    fun transition(order: Order, to: OrderStatus): Order {
        require(canTransition(order.status, to)) {
            "Invalid transition ${order.status} -> $to for order=${order.id}"
        }
        return order.copy(status = to)
    }
}
