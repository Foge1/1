package com.loaderapp.features.orders.data.mappers

import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignment
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime

private const val TIME_TYPE_EXACT = "exact"

fun OrderEntity.toDomain(
    applications: List<OrderApplication> = emptyList(),
    assignments: List<OrderAssignment> = emptyList()
): Order {
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
        workersCurrent = applications.count { it.status == OrderApplicationStatus.SELECTED },
        workersTotal = workersTotal,
        tags = tags,
        meta = meta,
        comment = comment,
        status = status.toOrderStatus(),
        createdByUserId = createdByUserId,
        applications = applications,
        assignments = assignments
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
        workersCurrent = applications.count { it.status == OrderApplicationStatus.SELECTED },
        workersTotal = workersTotal,
        tags = tags,
        meta = meta,
        comment = comment,
        status = status.toPersistedValue(),
        createdByUserId = createdByUserId
    )
}

fun OrderApplicationEntity.toDomain(): OrderApplication = OrderApplication(
    orderId = orderId,
    loaderId = loaderId,
    status = status.toOrderApplicationStatus(),
    appliedAtMillis = appliedAtMillis,
    ratingSnapshot = ratingSnapshot
)

fun OrderApplication.toEntity(): OrderApplicationEntity = OrderApplicationEntity(
    orderId = orderId,
    loaderId = loaderId,
    status = status.toPersistedValue(),
    appliedAtMillis = appliedAtMillis,
    ratingSnapshot = ratingSnapshot
)

fun OrderAssignmentEntity.toDomain(): OrderAssignment = OrderAssignment(
    orderId = orderId,
    loaderId = loaderId,
    status = status.toOrderAssignmentStatus(),
    assignedAtMillis = assignedAtMillis,
    startedAtMillis = startedAtMillis
)

fun OrderAssignment.toEntity(): OrderAssignmentEntity = OrderAssignmentEntity(
    orderId = orderId,
    loaderId = loaderId,
    status = status.toPersistedValue(),
    assignedAtMillis = assignedAtMillis,
    startedAtMillis = startedAtMillis
)

fun OrderStatus.toPersistedValue(): String = name
fun OrderApplicationStatus.toPersistedValue(): String = name
fun OrderAssignmentStatus.toPersistedValue(): String = name

fun String.toOrderStatus(): OrderStatus = runCatching { OrderStatus.valueOf(this) }.getOrDefault(OrderStatus.STAFFING)
fun String.toOrderApplicationStatus(): OrderApplicationStatus =
    runCatching { OrderApplicationStatus.valueOf(this) }.getOrDefault(OrderApplicationStatus.APPLIED)
fun String.toOrderAssignmentStatus(): OrderAssignmentStatus =
    runCatching { OrderAssignmentStatus.valueOf(this) }.getOrDefault(OrderAssignmentStatus.ACTIVE)
