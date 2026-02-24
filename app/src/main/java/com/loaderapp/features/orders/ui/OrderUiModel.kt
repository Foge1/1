package com.loaderapp.features.orders.ui

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderActionBlockReason
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.toDisplayMessage

data class OrderUiModel(
    val order: Order,
    val currentUserId: String,
    val currentUserRole: Role,
    val canApply: Boolean,
    val applyBlockReason: OrderActionBlockReason?,
    val canWithdraw: Boolean,
    val withdrawBlockReason: OrderActionBlockReason?,
    val canSelect: Boolean,
    val canUnselect: Boolean,
    val canStart: Boolean,
    val startBlockReason: OrderActionBlockReason?,
    val canCancel: Boolean,
    val cancelBlockReason: OrderActionBlockReason?,
    val canComplete: Boolean,
    val completeBlockReason: OrderActionBlockReason?,
    val canOpenChat: Boolean,
) {
    val applyDisabledReason: String?
        get() = applyBlockReason?.toDisplayMessage()

    val withdrawDisabledReason: String?
        get() = withdrawBlockReason?.toDisplayMessage()

    val startDisabledReason: String?
        get() = startBlockReason?.toDisplayMessage()

    val cancelDisabledReason: String?
        get() = cancelBlockReason?.toDisplayMessage()

    val completeDisabledReason: String?
        get() = completeBlockReason?.toDisplayMessage()

    val myApplication: OrderApplication?
        get() = order.applications.firstOrNull { it.loaderId == currentUserId }

    val myApplicationStatus: OrderApplicationStatus?
        get() = myApplication?.status

    val selectedApplicantsCount: Int
        get() = order.applications.count { it.status == OrderApplicationStatus.SELECTED }

    val appliedApplicantsCount: Int
        get() = order.applications.count { it.status == OrderApplicationStatus.APPLIED }

    val visibleApplicants: List<OrderApplication>
        get() = order.applications.filter {
            it.status == OrderApplicationStatus.APPLIED ||
                it.status == OrderApplicationStatus.SELECTED
        }
}

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
        comment = comment.orEmpty()
    )
}

fun OrderUiModel.toLegacyOrderModel(): OrderModel = order.toLegacyOrderModel()

private fun OrderStatus.toLegacyStatusModel(): OrderStatusModel = when (this) {
    OrderStatus.STAFFING -> OrderStatusModel.AVAILABLE
    OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
    OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
    OrderStatus.CANCELED,
    OrderStatus.EXPIRED -> OrderStatusModel.CANCELLED
}

private const val MIN_WORKER_RATING_KEY = "minWorkerRating"
private const val DISPATCHER_ID_KEY = "dispatcherId"
