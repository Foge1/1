package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.Order
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveOrdersUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {
    operator fun invoke(): Flow<List<Order>> = ordersRepository.observeOrders()
}
