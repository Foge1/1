package com.loaderapp.features.orders.data.mappers

import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.domain.model.OrderModel as LegacyOrderModel
import com.loaderapp.domain.model.OrderStatusModel as LegacyStatusModel
import com.loaderapp.features.orders.domain.Order as FeatureOrderModel
import com.loaderapp.features.orders.domain.OrderStatus as FeatureStatus

fun LegacyOrderModel.toFeatureOrderModel(): FeatureOrderModel =
    FeatureOrderModel(
        id = id,
        title = cargoDescription.ifBlank { "Заказ" },
        address = address,
        pricePerHour = pricePerHour,
        orderTime = if (isAsap) OrderTime.Soon else OrderTime.Exact(dateTime),
        durationMin = estimatedHours.coerceAtLeast(1) * 60,
        workersCurrent = if (workerId == null) 0 else 1,
        workersTotal = requiredWorkers,
        tags = listOf(cargoDescription),
        meta =
            mapOf(
                DISPATCHER_ID_KEY to dispatcherId.toString(),
                MIN_WORKER_RATING_KEY to minWorkerRating.toString(),
                FeatureOrderModel.CREATED_AT_KEY to createdAt.toString(),
            ),
        comment = comment,
        status = status.toFeatureStatus(),
        createdByUserId = dispatcherId.toString(),
    )

fun FeatureOrderModel.toOrderModel(): LegacyOrderModel = toLegacyOrderModel()

fun FeatureOrderModel.toLegacyOrderModel(): LegacyOrderModel {
    val durationHours = (durationMin / 60).coerceAtLeast(1)
    return LegacyOrderModel(
        id = id,
        address = address,
        dateTime = dateTime,
        cargoDescription = tags.firstOrNull() ?: title,
        pricePerHour = pricePerHour,
        estimatedHours = durationHours,
        requiredWorkers = workersTotal,
        minWorkerRating = meta[MIN_WORKER_RATING_KEY]?.toFloatOrNull() ?: 0f,
        status = status.toLegacyStatus(),
        createdAt = meta[FeatureOrderModel.CREATED_AT_KEY]?.toLongOrNull() ?: dateTime,
        completedAt = null,
        workerId = null,
        dispatcherId = meta[DISPATCHER_ID_KEY]?.toLongOrNull() ?: 0L,
        workerRating = null,
        comment = comment.orEmpty(),
        isAsap = orderTime is OrderTime.Soon,
    )
}

fun LegacyStatusModel.toFeatureStatus(): FeatureStatus =
    when (this) {
        LegacyStatusModel.AVAILABLE -> FeatureStatus.STAFFING
        LegacyStatusModel.TAKEN,
        LegacyStatusModel.IN_PROGRESS,
        -> FeatureStatus.IN_PROGRESS
        LegacyStatusModel.COMPLETED -> FeatureStatus.COMPLETED
        LegacyStatusModel.CANCELLED -> FeatureStatus.CANCELED
    }

fun FeatureStatus.toLegacyStatus(): LegacyStatusModel =
    when (this) {
        FeatureStatus.STAFFING -> LegacyStatusModel.AVAILABLE
        FeatureStatus.IN_PROGRESS -> LegacyStatusModel.IN_PROGRESS
        FeatureStatus.COMPLETED -> LegacyStatusModel.COMPLETED
        FeatureStatus.CANCELED,
        FeatureStatus.EXPIRED,
        -> LegacyStatusModel.CANCELLED
    }

private const val MIN_WORKER_RATING_KEY = "minWorkerRating"
private const val DISPATCHER_ID_KEY = "dispatcherId"
