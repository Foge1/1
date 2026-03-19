package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Параметры для получения истории заказов грузчика.
 * Поле называется [userId] — семантически верно для любой роли,
 * имплементация фильтрует по грузчику.
 */
data class GetOrderHistoryParams(
    val userId: Long,
)

/**
 * UseCase: Получить историю заказов грузчика.
 *
 * Возвращает завершённые и отменённые заказы,
 * отсортированные по дате завершения (новые первыми).
 */
class GetOrderHistoryUseCase
    @Inject
    constructor(
        private val orderRepository: OrderRepository,
    ) : FlowUseCase<GetOrderHistoryParams, Flow<List<OrderModel>>>() {
        override fun execute(params: GetOrderHistoryParams): Flow<List<OrderModel>> =
            orderRepository
                .getOrdersByWorker(params.userId)
                .map { orders ->
                    orders
                        .filter { it.status == OrderStatusModel.COMPLETED || it.status == OrderStatusModel.CANCELLED }
                        .sortedByDescending { it.completedAt ?: it.createdAt }
                }
    }
