package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.toDisplayMessage
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Грузчик откликается на заказ.
 *
 * Правила (канонические, Step 3):
 *  - Актор должен быть LOADER.
 *  - Не более 1 активного assignment одновременно.
 *  - Не более 3 активных откликов (APPLIED + SELECTED) одновременно.
 *  - Заказ должен быть в статусе STAFFING.
 *  - Грузчик ещё не откликнулся / не выбран в этот заказ.
 *
 * Мутации репозитория выполняются только после успешной проверки [OrderStateMachine.actionsFor].
 */
class ApplyToOrderUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long, now: Long = System.currentTimeMillis()): UseCaseResult<Unit> {
        val actor = currentUserProvider.getCurrentUser()

        if (actor.role != Role.LOADER) {
            return UseCaseResult.Failure("Только грузчик может откликнуться на заказ")
        }

        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        val context = OrderRulesContext(
            activeAssignmentExists = repository.hasActiveAssignment(actor.id),
            activeApplicationsForLimitCount = repository.countActiveApplicationsForLimit(actor.id),
            loaderHasActiveAssignmentInThisOrder = false
        )

        val actions = OrderStateMachine.actionsFor(order, actor, context)

        if (!actions.canApply) {
            return UseCaseResult.Failure(
                actions.applyDisabledReason?.toDisplayMessage() ?: "Нельзя откликнуться на этот заказ"
            )
        }

        return runCatching {
            repository.applyToOrder(orderId, actor.id, now)
            UseCaseResult.Success(Unit)
        }.getOrElse { e ->
            UseCaseResult.Failure(e.message ?: "Не удалось отправить отклик")
        }
    }
}
