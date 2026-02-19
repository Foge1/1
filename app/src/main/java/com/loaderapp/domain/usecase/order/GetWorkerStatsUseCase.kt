package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Параметры для получения статистики грузчика
 */
data class GetWorkerStatsParams(val workerId: Long)

/**
 * Статистика грузчика
 */
data class WorkerStats(
    val completedOrders: Int,
    val totalEarnings: Double,
    val averageRating: Float
)

/**
 * UseCase: Получить статистику грузчика
 * 
 * Объединяет данные из разных источников в единую модель
 */
class GetWorkerStatsUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetWorkerStatsParams, Flow<WorkerStats>>() {
    
    override fun execute(params: GetWorkerStatsParams): Flow<WorkerStats> {
        return combine(
            orderRepository.getCompletedOrdersCount(params.workerId),
            orderRepository.getTotalEarnings(params.workerId),
            orderRepository.getAverageRating(params.workerId)
        ) { completed, earnings, rating ->
            WorkerStats(
                completedOrders = completed,
                totalEarnings = earnings ?: 0.0,
                averageRating = rating ?: 5.0f
            )
        }
    }
}
