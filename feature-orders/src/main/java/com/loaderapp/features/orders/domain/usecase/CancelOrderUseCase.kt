package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderEvent
import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderTransitionResult
import com.loaderapp.features.orders.domain.toDisplayMessage
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Отменяет заказ.
 *
 * Правила: только диспетчер-создатель, в статусах STAFFING или IN_PROGRESS.
 * Все проверки делегируются [stateMachine.transition] — без дублирования бизнес-логики.
 */
class CancelOrderUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val stateMachine: OrderStateMachine
) {
    suspend operator fun invoke(orderId: Long, reason: String? = null): UseCaseResult<Unit> {
        if (reason != null && reason.isBlank()) {
            return UseCaseResult.Failure("Причина отмены не может быть пустой строкой")
        }

        val actor = currentUserProvider.requireCurrentUserOnce()
        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        val transitionResult = stateMachine.transition(
            order = order,
            event = OrderEvent.CANCEL,
            actor = actor,
            now = System.currentTimeMillis(),
            context = OrderRulesContext()
        )

        if (transitionResult is OrderTransitionResult.Failure) {
            return UseCaseResult.Failure(transitionResult.reason.toDisplayMessage())
        }

        return runCatchingUseCase("Не удалось отменить заказ") {
            repository.cancelOrder(orderId, reason)
            Unit
        }
    }
}
