package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Диспетчер-создатель выбирает грузчика из откликов.
 *
 * Правила: актор — DISPATCHER и создатель заказа, заказ — STAFFING.
 */
class SelectApplicantUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val stateMachine: OrderStateMachine
) {
    suspend operator fun invoke(orderId: Long, loaderId: String): UseCaseResult<Unit> {
        val actor = currentUserProvider.getCurrentUser()

        if (actor.role != Role.DISPATCHER) {
            return UseCaseResult.Failure("Только диспетчер может выбирать грузчиков")
        }

        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        val actions = stateMachine.actionsFor(order, actor)

        if (!actions.canSelect) {
            return UseCaseResult.Failure("Нет прав для выбора грузчика в этом заказе")
        }

        return runCatching {
            repository.selectApplicant(orderId, loaderId)
            UseCaseResult.Success(Unit)
        }.getOrElse { e ->
            UseCaseResult.Failure(e.message ?: "Не удалось выбрать грузчика")
        }
    }
}
