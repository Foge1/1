package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Параметры для получения заказов диспетчера
 */
data class GetOrdersByDispatcherParams(val dispatcherId: Long)

/**
 * UseCase: Получить заказы конкретного диспетчера
 */
class GetOrdersByDispatcherUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetOrdersByDispatcherParams, Flow<List<OrderModel>>>() {
    
    override fun execute(params: GetOrdersByDispatcherParams): Flow<List<OrderModel>> {
        return orderRepository.getOrdersByDispatcher(params.dispatcherId)
    }
}
