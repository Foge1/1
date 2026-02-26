package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.toDisplayMessage
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * Диспетчер-создатель запускает заказ: STAFFING → IN_PROGRESS.
 */
class StartOrderUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val stateMachine: OrderStateMachine
) {
    suspend fun start(orderId: Long, now: Long = System.currentTimeMillis()): StartOrderResult {
        val actor = currentUserProvider.requireCurrentUserOnce()

        if (actor.role != Role.DISPATCHER) {
            return StartOrderResult.Forbidden("Только диспетчер может запустить заказ")
        }

        val order = repository.getOrderById(orderId)
            ?: return StartOrderResult.OrderNotFound

        val actions = stateMachine.actionsFor(order, actor)

        if (!actions.canStart) {
            return StartOrderResult.Forbidden(
                actions.startDisabledReason?.toDisplayMessage() ?: "Невозможно запустить заказ"
            )
        }

        val selectedLoaderIds = order.applications
            .asSequence()
            .filter { it.status == OrderApplicationStatus.SELECTED }
            .map { it.loaderId }
            .distinct()
            .toList()

        val busyAssignments = repository.getBusyAssignments(selectedLoaderIds)
            .filterValues { activeOrderId -> activeOrderId != orderId }

        val conflict = busyAssignments.entries.firstOrNull()
        if (conflict != null) {
            return StartOrderResult.AssigneeAlreadyBusy(conflict.key, conflict.value)
        }

        repository.startOrder(orderId, now)
        return StartOrderResult.Success
    }

    suspend operator fun invoke(orderId: Long, now: Long = System.currentTimeMillis()): UseCaseResult<Unit> {
        return try {
            when (val result = start(orderId, now)) {
                StartOrderResult.Success -> UseCaseResult.Success(Unit)
                StartOrderResult.OrderNotFound -> UseCaseResult.Failure("Заказ не найден")
                is StartOrderResult.Forbidden -> UseCaseResult.Failure(result.reason)
                is StartOrderResult.AssigneeAlreadyBusy -> UseCaseResult.Failure("Грузчик уже в работе на другом заказе")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            UseCaseResult.Failure(e.message ?: "Не удалось запустить заказ")
        }
    }
}

sealed interface StartOrderResult {
    data object Success : StartOrderResult
    data object OrderNotFound : StartOrderResult
    data class Forbidden(val reason: String) : StartOrderResult
    data class AssigneeAlreadyBusy(val userId: String, val activeOrderId: Long) : StartOrderResult
}
