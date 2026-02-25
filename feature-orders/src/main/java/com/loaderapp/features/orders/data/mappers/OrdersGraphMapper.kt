package com.loaderapp.features.orders.data.mappers

import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import com.loaderapp.features.orders.domain.Order
import javax.inject.Inject

class OrdersGraphMapper @Inject constructor() {

    fun toDomainOrders(
        orderEntities: List<OrderEntity>,
        applicationEntities: List<OrderApplicationEntity>,
        assignmentEntities: List<OrderAssignmentEntity>
    ): List<Order> {
        val appsByOrder = applicationEntities.groupBy { it.orderId }
        val assignmentsByOrder = assignmentEntities.groupBy { it.orderId }

        return orderEntities.map { entity ->
            entity.toDomain(
                applications = appsByOrder[entity.id].orEmpty().map { it.toDomain() },
                assignments = assignmentsByOrder[entity.id].orEmpty().map { it.toDomain() }
            )
        }
    }
}
