package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.toDisplayMessage
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Диспетчер-создатель запускает заказ: STAFFING → IN_PROGRESS.
 *
 * Правила: актор — DISPATCHER-создатель, selectedCount == workersTotal.
 * Репозиторий создаёт ACTIVE assignments для всех SELECTED applicants
 * и переводит заказ в IN_PROGRESS.
 */
class StartOrderUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val stateMachine: OrderStateMachine
) {
    suspend operator fun invoke(orderId: Long, now: Long = System.currentTimeMillis()): UseCaseResult<Unit> {
        val actor = currentUserProvider.getCurrentUser()

        if (actor.role != Role.DISPATCHER) {
            return UseCaseResult.Failure("Только диспетчер может запустить заказ")
        }

        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        val actions = stateMachine.actionsFor(order, actor)

        if (!actions.canStart) {
            return UseCaseResult.Failure(
                actions.startDisabledReason?.toDisplayMessage() ?: "Невозможно запустить заказ"
            )
        }

        return runCatching {
            repository.startOrder(orderId, now)
            UseCaseResult.Success(Unit)
        }.getOrElse { e ->
            UseCaseResult.Failure(e.message ?: "Не удалось запустить заказ")
        }
    }
}
