package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

class CancelOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long, reason: String? = null): UseCaseResult<Unit> {
        if (reason != null && reason.isBlank()) {
            return UseCaseResult.Failure("Причина отмены не может быть пустой")
        }

        val currentUser = currentUserProvider.getCurrentUser()
        val order = ordersRepository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        if (order.status == OrderStatus.CANCELED) {
            return UseCaseResult.Success(Unit)
        }

        val canCancel = when (currentUser.role) {
            Role.DISPATCHER -> order.createdByUserId == currentUser.id &&
                (order.status == OrderStatus.AVAILABLE || order.status == OrderStatus.IN_PROGRESS)
            Role.LOADER -> order.status == OrderStatus.IN_PROGRESS && order.acceptedByUserId == currentUser.id
        }
        if (!canCancel) {
            return UseCaseResult.Failure("Недостаточно прав для отмены заказа")
        }

        if (!OrderStateMachine.canTransition(order.status, OrderStatus.CANCELED)) {
            return UseCaseResult.Failure("Нельзя отменить заказ в статусе ${order.status}")
        }

        return runCatching {
            ordersRepository.cancelOrder(orderId, reason)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось отменить заказ")
        }
    }
}
