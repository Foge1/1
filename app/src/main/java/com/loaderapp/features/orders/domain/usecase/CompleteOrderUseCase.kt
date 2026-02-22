package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

class CompleteOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val currentUser = currentUserProvider.getCurrentUser()
        val order = ordersRepository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        if (order.status == OrderStatus.COMPLETED) {
            return UseCaseResult.Success(Unit)
        }

        val canComplete = currentUser.role == Role.LOADER &&
            order.status == OrderStatus.IN_PROGRESS &&
            order.acceptedByUserId == currentUser.id
        if (!canComplete) {
            return UseCaseResult.Failure("Недостаточно прав для завершения заказа")
        }

        if (!OrderStateMachine.canTransition(order.status, OrderStatus.COMPLETED)) {
            return UseCaseResult.Failure("Нельзя завершить заказ в статусе ${order.status}")
        }

        return runCatching {
            ordersRepository.completeOrder(orderId)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось завершить заказ")
        }
    }
}
