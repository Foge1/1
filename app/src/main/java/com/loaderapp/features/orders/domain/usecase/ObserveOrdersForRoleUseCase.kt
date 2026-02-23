package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ObserveOrdersForRoleUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    operator fun invoke(): Flow<List<Order>> {
        return currentUserProvider.observeCurrentUser()
            .flatMapLatest { user ->
                ordersRepository.observeOrders().map { orders -> orders.filterForUser(user) }
            }
    }
}

private fun List<Order>.filterForUser(user: CurrentUser): List<Order> {
    return when (user.role) {
        Role.DISPATCHER -> filter { order -> order.createdByUserId == user.id }
        Role.LOADER -> filter { order ->
            when (order.status) {
                OrderStatus.STAFFING -> true
                OrderStatus.IN_PROGRESS,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELED,
                OrderStatus.EXPIRED -> order.assignments.any { it.loaderId == user.id }
            }
        }
    }
}
