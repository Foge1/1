package com.loaderapp.features.orders.presentation.mapper

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.presentation.model.OrderCardUiModel
import com.loaderapp.features.orders.ui.OrderUiModel

fun Order.toUiModel(): OrderCardUiModel {
    val currentWorkers = assignments.count { assignment -> assignment.status == OrderAssignmentStatus.ACTIVE }
    return OrderCardUiModel(
        id = id,
        address = address,
        dateTime = dateTime,
        cargoDescription = tags.firstOrNull() ?: title,
        pricePerHour = pricePerHour,
        estimatedHours = (durationMin / 60).coerceAtLeast(1),
        requiredWorkers = workersTotal,
        currentWorkers = currentWorkers,
        status = status.toLegacyStatusModel(),
        comment = comment.orEmpty(),
        minWorkerRating = meta[MIN_WORKER_RATING_KEY]?.toFloatOrNull() ?: 0f,
        dispatcherId = meta[DISPATCHER_ID_KEY]?.toLongOrNull() ?: 0L,
        isAsap = orderTime is OrderTime.Soon
    )
}

fun OrderUiModel.toUiModel(): OrderCardUiModel {
    val selectedWorkers = order.applications.count { it.status == OrderApplicationStatus.SELECTED }
    val activeAssignments = order.assignments.count { it.status == OrderAssignmentStatus.ACTIVE }
    return order.toUiModel().copy(currentWorkers = maxOf(activeAssignments, selectedWorkers))
}

fun Order.toLegacyOrderModel(): OrderModel = toUiModel().toLegacyOrderModel()

fun Order.toOrderModel(): OrderModel = toLegacyOrderModel()

fun OrderUiModel.toLegacyOrderModel(): OrderModel = toUiModel().toLegacyOrderModel()

fun OrderCardUiModel.toLegacyOrderModel(): OrderModel = OrderModel(
    id = id,
    address = address,
    dateTime = dateTime,
    cargoDescription = cargoDescription,
    pricePerHour = pricePerHour,
    estimatedHours = estimatedHours,
    requiredWorkers = requiredWorkers,
    minWorkerRating = minWorkerRating,
    status = status,
    createdAt = dateTime,
    completedAt = null,
    workerId = null,
    dispatcherId = dispatcherId,
    workerRating = null,
    comment = comment,
    isAsap = isAsap
)

private fun OrderStatus.toLegacyStatusModel(): OrderStatusModel = when (this) {
    OrderStatus.STAFFING -> OrderStatusModel.AVAILABLE
    OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
    OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
    OrderStatus.CANCELED,
    OrderStatus.EXPIRED -> OrderStatusModel.CANCELLED
}

private const val MIN_WORKER_RATING_KEY = "minWorkerRating"
private const val DISPATCHER_ID_KEY = "dispatcherId"
