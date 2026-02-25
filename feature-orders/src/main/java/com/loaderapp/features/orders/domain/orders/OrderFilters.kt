package com.loaderapp.features.orders.domain.orders

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.session.CurrentUser

internal fun List<Order>.filterForUser(user: CurrentUser): List<Order> =
    when (user.role) {
        Role.DISPATCHER -> filter { order -> order.createdByUserId == user.id }
        Role.LOADER -> filter { order ->
            when (order.status) {
                OrderStatus.STAFFING -> true
                OrderStatus.IN_PROGRESS,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELED,
                OrderStatus.EXPIRED -> {
                    order.assignments.any { it.loaderId == user.id }
                }
            }
        }
    }
