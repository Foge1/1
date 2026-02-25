package com.loaderapp.features.orders.data.mappers

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime

fun OrderModel.toFeatureStatus(): OrderStatus = when (status) {
    OrderStatusModel.AVAILABLE -> OrderStatus.STAFFING
    OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> OrderStatus.IN_PROGRESS
    OrderStatusModel.COMPLETED -> OrderStatus.COMPLETED
    OrderStatusModel.CANCELLED -> OrderStatus.CANCELED
}

fun Order.toLegacyOrderModel(): OrderModel = toOrderModel()

fun Order.toOrderModel(): OrderModel {
    val durationHours = (durationMin / 60).coerceAtLeast(1)
    return OrderModel(
        id = id,
        address = address,
        dateTime = dateTime,
        cargoDescription = tags.firstOrNull() ?: title,
        pricePerHour = pricePerHour,
        estimatedHours = durationHours,
        requiredWorkers = workersTotal,
        minWorkerRating = meta[MIN_WORKER_RATING_KEY]?.toFloatOrNull() ?: 0f,
        status = status.toLegacyStatusModel(),
        createdAt = meta[Order.CREATED_AT_KEY]?.toLongOrNull() ?: dateTime,
        completedAt = null,
        workerId = null,
        dispatcherId = meta[DISPATCHER_ID_KEY]?.toLongOrNull() ?: 0L,
        workerRating = null,
        comment = comment.orEmpty(),
        isAsap = orderTime is OrderTime.Soon
    )
}

private fun OrderStatus.toLegacyStatusModel(): OrderStatusModel = when (this) {
    OrderStatus.STAFFING -> OrderStatusModel.AVAILABLE
    OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
    OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
    OrderStatus.CANCELED,
    OrderStatus.EXPIRED -> OrderStatusModel.CANCELLED
}

private const val MIN_WORKER_RATING_KEY = "minWorkerRating"
private const val DISPATCHER_ID_KEY = "dispatcherId"
