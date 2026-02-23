package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderActions
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.ui.OrderUiModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

/**
 * Единая точка построения [OrderUiModel] из потоков пользователя и заказов.
 */
class ObserveOrderUiModelsUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
) {
    operator fun invoke(): Flow<List<OrderUiModel>> =
        combine(
            currentUserProvider.observeCurrentUser(),
            repository.observeOrders()
        ) { actor, orders ->
            actor to orders
        }.mapLatest { (actor, orders) ->
            val visibleOrders = orders.filterForUser(actor)
            val baseContext = buildBaseContext(actor)
            visibleOrders.map { order -> order.toUiModel(actor, baseContext) }
        }

    private suspend fun buildBaseContext(actor: CurrentUser): OrderRulesContext =
        when (actor.role) {
            Role.LOADER -> OrderRulesContext(
                activeAssignmentExists = repository.hasActiveAssignment(actor.id),
                activeApplicationsForLimitCount = repository.countActiveApplicationsForLimit(actor.id),
                loaderHasActiveAssignmentInThisOrder = false
            )
            Role.DISPATCHER -> OrderRulesContext()
        }

    private fun Order.toUiModel(actor: CurrentUser, baseContext: OrderRulesContext): OrderUiModel {
        val hasAssignmentHere = actor.role == Role.LOADER &&
            assignments.any { it.loaderId == actor.id && it.status == OrderAssignmentStatus.ACTIVE }

        val context = baseContext.copy(loaderHasActiveAssignmentInThisOrder = hasAssignmentHere)
        val actions: OrderActions = OrderStateMachine.actionsFor(this, actor, context)

        return OrderUiModel(
            order = this,
            currentUserId = actor.id,
            currentUserRole = actor.role,
            canApply = actions.canApply,
            applyBlockReason = actions.applyDisabledReason,
            canWithdraw = actions.canWithdraw,
            withdrawBlockReason = actions.withdrawDisabledReason,
            canSelect = actions.canSelect,
            canUnselect = actions.canUnselect,
            canStart = actions.canStart,
            startBlockReason = actions.startDisabledReason,
            canCancel = actions.canCancel,
            cancelBlockReason = actions.cancelDisabledReason,
            canComplete = actions.canComplete,
            completeBlockReason = actions.completeDisabledReason,
            canOpenChat = actions.canOpenChat,
        )
    }
}

private fun List<Order>.filterForUser(user: CurrentUser): List<Order> =
    when (user.role) {
        Role.DISPATCHER -> filter { order -> order.createdByUserId == user.id }
        Role.LOADER -> filter { order ->
            when (order.status) {
                OrderStatus.STAFFING -> true
                OrderStatus.IN_PROGRESS,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELED,
                OrderStatus.EXPIRED -> {
                    order.assignments.any { it.loaderId == user.id }
                }
            }
        }
    }
