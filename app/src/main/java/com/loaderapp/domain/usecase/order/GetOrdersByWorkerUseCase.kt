package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Параметры для получения заказов грузчика
 */
data class GetOrdersByWorkerParams(val workerId: Long)

/**
 * UseCase: Получить заказы конкретного грузчика
 */
class GetOrdersByWorkerUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetOrdersByWorkerParams, Flow<List<OrderModel>>>() {
    
    override suspend fun execute(params: GetOrdersByWorkerParams): Flow<List<OrderModel>> {
        return orderRepository.getOrdersByWorker(params.workerId)
    }
}
