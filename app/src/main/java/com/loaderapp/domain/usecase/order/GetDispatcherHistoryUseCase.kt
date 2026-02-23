package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Параметры для получения истории заказов диспетчера
 */
data class GetDispatcherHistoryParams(val dispatcherId: Long)

/**
 * UseCase: Получить историю заказов диспетчера.
 *
 * Возвращает завершённые и отменённые заказы диспетчера,
 * отсортированные по дате завершения (новые первыми).
 */
class GetDispatcherHistoryUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetDispatcherHistoryParams, Flow<List<OrderModel>>>() {

    override fun execute(params: GetDispatcherHistoryParams): Flow<List<OrderModel>> =
        orderRepository.getAllOrders()
            .map { orders ->
                orders
                    .filter { it.dispatcherId == params.dispatcherId }
                    .filter { it.status == OrderStatusModel.COMPLETED || it.status == OrderStatusModel.CANCELLED }
                    .sortedByDescending { it.completedAt ?: it.createdAt }
            }
}
