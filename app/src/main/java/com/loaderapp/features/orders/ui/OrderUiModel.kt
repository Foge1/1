package com.loaderapp.features.orders.ui

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus

data class OrderUiModel(
    val order: Order,
    val canAccept: Boolean,
    val canCancel: Boolean,
    val canComplete: Boolean,
    val canOpenChat: Boolean
)

fun Order.toUiModel(): OrderUiModel {
    val actions = OrderStateMachine.actionsFor(this)
    return OrderUiModel(this, actions.canAccept, actions.canCancel, actions.canComplete, actions.canOpenChat)
}

fun OrderUiModel.toLegacyOrderModel(): OrderModel {
    val status = when (order.status) {
        OrderStatus.AVAILABLE -> OrderStatusModel.AVAILABLE
        OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
        OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
        OrderStatus.CANCELED,
        OrderStatus.EXPIRED -> OrderStatusModel.CANCELLED
    }

    val durationHours = (order.durationMin / 60).coerceAtLeast(1)

    return OrderModel(
        id = order.id,
        address = order.address,
        dateTime = order.dateTime,
        cargoDescription = order.tags.firstOrNull() ?: order.title,
        pricePerHour = order.pricePerHour,
        estimatedHours = durationHours,
        requiredWorkers = order.workersTotal,
        minWorkerRating = 0f,
        status = status,
        createdAt = order.dateTime,
        completedAt = null,
        workerId = if (order.workersCurrent > 0) 1L else null,
        dispatcherId = 0L,
        workerRating = null,
        comment = order.comment.orEmpty()
    )
}
