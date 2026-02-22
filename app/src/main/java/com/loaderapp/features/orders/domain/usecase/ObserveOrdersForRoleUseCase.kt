package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ObserveOrdersForRoleUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    operator fun invoke(): Flow<List<Order>> = flow {
        val user = currentUserProvider.getCurrentUser()
        emitAll(
            ordersRepository.observeOrders().map { orders ->
                when (user.role) {
                    Role.DISPATCHER -> orders.filter { order -> order.createdByUserId == user.id }
                    Role.LOADER -> orders.filter { order ->
                        when (order.status) {
                            OrderStatus.AVAILABLE -> order.acceptedByUserId == null
                            OrderStatus.IN_PROGRESS -> order.acceptedByUserId == user.id
                            OrderStatus.COMPLETED,
                            OrderStatus.CANCELED,
                            OrderStatus.EXPIRED -> order.acceptedByUserId == user.id
                        }
                    }
                }
            }
        )
    }
}
