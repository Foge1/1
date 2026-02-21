package com.loaderapp.features.orders.domain

object OrderStateMachine {
    private val allowedTransitions = mapOf(
        OrderStatus.AVAILABLE to setOf(OrderStatus.IN_PROGRESS, OrderStatus.CANCELED, OrderStatus.EXPIRED),
        OrderStatus.IN_PROGRESS to setOf(OrderStatus.COMPLETED, OrderStatus.CANCELED),
        OrderStatus.COMPLETED to emptySet(),
        OrderStatus.CANCELED to emptySet(),
        OrderStatus.EXPIRED to emptySet()
    )

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
        return to in (allowedTransitions[from] ?: emptySet())
    }

    fun transition(order: Order, newStatus: OrderStatus): OrderTransitionResult {
        val allowed = allowedTransitions[order.status] ?: emptySet()

        if (newStatus !in allowed) {
            return OrderTransitionResult.Failure(
                "Invalid transition ${order.status} → $newStatus"
            )
        }

        return OrderTransitionResult.Success(
            order.copy(status = newStatus)
        )
    }
}
