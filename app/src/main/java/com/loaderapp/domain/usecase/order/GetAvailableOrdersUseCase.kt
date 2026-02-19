package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase: Получить список доступных заказов
 */
class GetAvailableOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<Unit, Flow<List<OrderModel>>>() {
    
    override suspend fun execute(params: Unit): Flow<List<OrderModel>> {
        return orderRepository.getAvailableOrders()
    }
}
