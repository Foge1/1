package com.loaderapp.features.orders.testing

import com.loaderapp.features.orders.domain.OrderAssignment
import com.loaderapp.features.orders.domain.OrderAssignmentStatus

object OrderAssignmentTestFactory {
    fun assignment(
        orderId: Long,
        loaderId: String,
        status: OrderAssignmentStatus = OrderAssignmentStatus.ACTIVE,
        assignedAtMillis: Long = 1_000L,
        startedAtMillis: Long? = null
    ): OrderAssignment = OrderAssignment(
        orderId = orderId,
        loaderId = loaderId,
        status = status,
        assignedAtMillis = assignedAtMillis,
        startedAtMillis = startedAtMillis
    )
}
