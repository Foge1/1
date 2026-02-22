package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

class AcceptOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val currentUser = currentUserProvider.getCurrentUser()
        val order = ordersRepository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        if (order.acceptedByUserId != null && order.acceptedByUserId != currentUser.id) {
            return UseCaseResult.Failure("Заказ уже взяли")
        }

        if (order.status == OrderStatus.IN_PROGRESS && order.acceptedByUserId == currentUser.id) {
            return UseCaseResult.Success(Unit)
        }

        if (!OrderStateMachine.canTransition(order.status, OrderStatus.IN_PROGRESS)) {
            return UseCaseResult.Failure("Нельзя принять заказ в статусе ${order.status}")
        }

        return runCatching {
            ordersRepository.acceptOrder(orderId, currentUser.id, System.currentTimeMillis())
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось принять заказ")
        }
    }
}
