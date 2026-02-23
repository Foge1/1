package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderEvent
import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderTransitionResult
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.toDisplayMessage
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Завершает заказ (IN_PROGRESS → COMPLETED).
 *
 * Правила:
 *  - Диспетчер-создатель — всегда может завершить.
 *  - Грузчик — только если у него есть ACTIVE assignment в этом заказе.
 *
 * Контекст [OrderRulesContext.loaderHasActiveAssignmentInThisOrder] вычисляется здесь
 * и передаётся в [OrderStateMachine] — единственный источник правил.
 */
class CompleteOrderUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val actor = currentUserProvider.getCurrentUser()
        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        val loaderHasActiveAssignment = actor.role == Role.LOADER &&
            order.assignments.any {
                it.loaderId == actor.id && it.status == OrderAssignmentStatus.ACTIVE
            }

        val context = OrderRulesContext(
            loaderHasActiveAssignmentInThisOrder = loaderHasActiveAssignment
        )

        val transitionResult = OrderStateMachine.transition(
            order = order,
            event = OrderEvent.COMPLETE,
            actor = actor,
            now = System.currentTimeMillis(),
            context = context
        )

        if (transitionResult is OrderTransitionResult.Failure) {
            return UseCaseResult.Failure(transitionResult.reason.toDisplayMessage())
        }

        return runCatching {
            repository.completeOrder(orderId)
            UseCaseResult.Success(Unit)
        }.getOrElse { e ->
            UseCaseResult.Failure(e.message ?: "Не удалось завершить заказ")
        }
    }
}
