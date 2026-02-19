package com.loaderapp.domain.usecase.order

import com.loaderapp.core.common.Result
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Параметры для завершения заказа
 */
data class CompleteOrderParams(val orderId: Long)

/**
 * UseCase: Завершить заказ
 * 
 * Бизнес-правила:
 * - Заказ должен быть в статусе TAKEN или IN_PROGRESS
 */
class CompleteOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : UseCase<CompleteOrderParams, Unit>() {
    
    override suspend fun execute(params: CompleteOrderParams): Result<Unit> {
        // Получаем заказ
        val orderResult = orderRepository.getOrderById(params.orderId)
        if (orderResult is Result.Error) {
            return Result.Error("Заказ не найден")
        }
        val order = orderResult.getOrNull() ?: return Result.Error("Заказ не найден")
        
        // Проверка возможности завершения
        if (!order.canBeCompleted()) {
            return Result.Error("Невозможно завершить заказ в текущем статусе: ${order.status.getDisplayName()}")
        }
        
        // Завершение заказа
        return orderRepository.completeOrder(params.orderId)
    }
}
