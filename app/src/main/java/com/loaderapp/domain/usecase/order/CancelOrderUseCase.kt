package com.loaderapp.domain.usecase.order

import com.loaderapp.core.common.Result
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Параметры для отмены заказа
 */
data class CancelOrderParams(val orderId: Long)

/**
 * UseCase: Отменить заказ
 * 
 * Бизнес-правила:
 * - Заказ не должен быть COMPLETED или CANCELLED
 */
class CancelOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : UseCase<CancelOrderParams, Unit>() {
    
    override suspend fun execute(params: CancelOrderParams): Result<Unit> {
        // Получаем заказ
        val orderResult = orderRepository.getOrderById(params.orderId)
        if (orderResult is Result.Error) {
            return Result.Error("Заказ не найден")
        }
        val order = orderResult.getOrNull() ?: return Result.Error("Заказ не найден")
        
        // Проверка возможности отмены
        if (!order.canBeCancelled()) {
            return Result.Error("Невозможно отменить заказ в статусе: ${order.status.getDisplayName()}")
        }
        
        // Отмена заказа
        return orderRepository.cancelOrder(params.orderId)
    }
}
