package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderStateMachine
import javax.inject.Inject

class AcceptOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val order = ordersRepository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        if (order.status == OrderStatus.IN_PROGRESS) {
            return UseCaseResult.Success(Unit)
        }

        if (!OrderStateMachine.canTransition(order.status, OrderStatus.IN_PROGRESS)) {
            return UseCaseResult.Failure("Нельзя принять заказ в статусе ${order.status}")
        }

        return runCatching {
            ordersRepository.acceptOrder(orderId)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось принять заказ")
        }
    }
}
