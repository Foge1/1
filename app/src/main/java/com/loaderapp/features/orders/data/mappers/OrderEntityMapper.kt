package com.loaderapp.features.orders.data.mappers

import com.loaderapp.features.orders.data.local.entity.OrderEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime

private const val TIME_TYPE_EXACT = "exact"

fun OrderEntity.toDomain(): Order {
    val time = when (orderTimeType) {
        Order.TIME_TYPE_SOON -> OrderTime.Soon
        else -> OrderTime.Exact(orderTimeExactMillis ?: 0L)
    }

    return Order(
        id = id,
        title = title,
        address = address,
        pricePerHour = pricePerHour,
        orderTime = time,
        durationMin = durationMin,
        workersCurrent = workersCurrent,
        workersTotal = workersTotal,
        tags = tags,
        meta = meta,
        comment = comment,
        status = OrderStatus.valueOf(status),
        createdByUserId = createdByUserId,
        acceptedByUserId = acceptedByUserId,
        acceptedAtMillis = acceptedAtMillis
    )
}

fun Order.toEntity(): OrderEntity {
    val (timeType, exactMillis) = when (val time = orderTime) {
        is OrderTime.Exact -> TIME_TYPE_EXACT to time.dateTimeMillis
        OrderTime.Soon -> Order.TIME_TYPE_SOON to null
    }

    return OrderEntity(
        id = id,
        title = title,
        address = address,
        pricePerHour = pricePerHour,
        orderTimeType = timeType,
        orderTimeExactMillis = exactMillis,
        durationMin = durationMin,
        workersCurrent = workersCurrent,
        workersTotal = workersTotal,
        tags = tags,
        meta = meta,
        comment = comment,
        status = status.name,
        createdByUserId = createdByUserId,
        acceptedByUserId = acceptedByUserId,
        acceptedAtMillis = acceptedAtMillis
    )
}
