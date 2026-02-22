package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderEvent
import com.loaderapp.features.orders.domain.OrderTransitionResult
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

        val transitionResult = OrderStateMachine.transition(
            order = order,
            event = OrderEvent.ACCEPT,
            actor = currentUser,
            now = System.currentTimeMillis()
        )

        val transitionedOrder = when (transitionResult) {
            is OrderTransitionResult.Success -> transitionResult.order
            is OrderTransitionResult.Failure -> return UseCaseResult.Failure(transitionResult.reason)
        }

        val acceptedByUserId = transitionedOrder.acceptedByUserId
            ?: return UseCaseResult.Failure("Order assignee is missing")
        val acceptedAtMillis = transitionedOrder.acceptedAtMillis
            ?: return UseCaseResult.Failure("Order acceptance timestamp is missing")

        return runCatching {
            ordersRepository.acceptOrder(orderId, acceptedByUserId, acceptedAtMillis)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось принять заказ")
        }
    }
}
