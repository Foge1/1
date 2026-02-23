package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Грузчик отзывает свой отклик.
 *
 * Правила: актор — LOADER, у него есть активный отклик (APPLIED или SELECTED),
 * заказ — в статусе STAFFING.
 */
class WithdrawApplicationUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val actor = currentUserProvider.getCurrentUser()

        if (actor.role != Role.LOADER) {
            return UseCaseResult.Failure("Только грузчик может отозвать отклик")
        }

        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        val actions = OrderStateMachine.actionsFor(order, actor, OrderRulesContext())

        if (!actions.canWithdraw) {
            return UseCaseResult.Failure(
                actions.withdrawDisabledReason ?: "Нельзя отозвать отклик"
            )
        }

        return runCatching {
            repository.withdrawApplication(orderId, actor.id)
            UseCaseResult.Success(Unit)
        }.getOrElse { e ->
            UseCaseResult.Failure(e.message ?: "Не удалось отозвать отклик")
        }
    }
}
