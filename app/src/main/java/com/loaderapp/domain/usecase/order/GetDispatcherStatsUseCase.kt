package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Параметры для получения статистики диспетчера
 */
data class GetDispatcherStatsParams(val dispatcherId: Long)

/**
 * Статистика диспетчера
 */
data class DispatcherStats(
    val completedOrders: Int,
    val activeOrders: Int
)

/**
 * UseCase: Получить статистику диспетчера
 */
class GetDispatcherStatsUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetDispatcherStatsParams, Flow<DispatcherStats>>() {
    
    override suspend fun execute(params: GetDispatcherStatsParams): Flow<DispatcherStats> {
        return combine(
            orderRepository.getDispatcherCompletedCount(params.dispatcherId),
            orderRepository.getDispatcherActiveCount(params.dispatcherId)
        ) { completed, active ->
            DispatcherStats(
                completedOrders = completed,
                activeOrders = active
            )
        }
    }
}
