package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderEvent
import com.loaderapp.features.orders.domain.OrderTransitionResult
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

        val transitionResult = OrderStateMachine.transition(
            order = order,
            event = OrderEvent.CANCEL,
            actor = currentUser,
            now = System.currentTimeMillis()
        )

        if (transitionResult is OrderTransitionResult.Failure) {
            return UseCaseResult.Failure(transitionResult.reason)
        }

        return runCatching {
            ordersRepository.cancelOrder(orderId, reason)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось отменить заказ")
        }
    }
}
