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
    suspend fun select(orderId: Long, loaderId: String): SelectApplicantResult {
        val actor = currentUserProvider.requireCurrentUserOnce()

        if (actor.role != Role.DISPATCHER) {
            return SelectApplicantResult.Forbidden("Только диспетчер может выбирать грузчиков")
        }

        val order = repository.getOrderById(orderId)
            ?: return SelectApplicantResult.OrderNotFound

        val actions = stateMachine.actionsFor(order, actor)

        if (!actions.canSelect) {
            return SelectApplicantResult.Forbidden("Нет прав для выбора грузчика в этом заказе")
        }

        val hasBusyAssignment = repository.hasActiveAssignment(loaderId)
        val assignmentInCurrentOrder = repository.hasActiveAssignmentInOrder(orderId, loaderId)
        if (hasBusyAssignment && !assignmentInCurrentOrder) {
            return SelectApplicantResult.AlreadyAssigned
        }

        repository.selectApplicant(orderId, loaderId)
        return SelectApplicantResult.Success
    }

    suspend operator fun invoke(orderId: Long, loaderId: String): UseCaseResult<Unit> {
        return runCatching {
            when (val result = select(orderId, loaderId)) {
                SelectApplicantResult.Success -> UseCaseResult.Success(Unit)
                SelectApplicantResult.OrderNotFound -> UseCaseResult.Failure("Заказ не найден")
                SelectApplicantResult.AlreadyAssigned -> UseCaseResult.Failure("Грузчик уже в работе на другом заказе")
                is SelectApplicantResult.Forbidden -> UseCaseResult.Failure(result.reason)
            }
        }.getOrElse { e ->
            UseCaseResult.Failure(e.message ?: "Не удалось выбрать грузчика")
        }
    }
}

sealed interface SelectApplicantResult {
    data object Success : SelectApplicantResult
    data object OrderNotFound : SelectApplicantResult
    data object AlreadyAssigned : SelectApplicantResult
    data class Forbidden(val reason: String) : SelectApplicantResult
}
