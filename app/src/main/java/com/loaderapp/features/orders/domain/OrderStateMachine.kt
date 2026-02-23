package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser

data class OrderRulesContext(
    val activeAssignmentExists: Boolean = false,
    val activeApplicationsForLimitCount: Int = 0,
    val loaderHasActiveAssignmentInThisOrder: Boolean = false
)

data class OrderActions(
    val canApply: Boolean = false,
    val applyDisabledReason: OrderActionBlockReason? = null,
    val canWithdraw: Boolean = false,
    val withdrawDisabledReason: OrderActionBlockReason? = null,
    val canSelect: Boolean = false,
    val canUnselect: Boolean = false,
    val canStart: Boolean = false,
    val startDisabledReason: OrderActionBlockReason? = null,
    val canCancel: Boolean = false,
    val cancelDisabledReason: OrderActionBlockReason? = null,
    val canComplete: Boolean = false,
    val completeDisabledReason: OrderActionBlockReason? = null,
    val canOpenChat: Boolean = false
)

private const val MAX_ACTIVE_APPLICATIONS = 3

object OrderStateMachine {

    fun actionsFor(
        order: Order,
        actor: CurrentUser,
        context: OrderRulesContext = OrderRulesContext()
    ): OrderActions = when (actor.role) {
        Role.LOADER -> loaderActionsFor(order, actor, context)
        Role.DISPATCHER -> dispatcherActionsFor(order, actor)
    }

    fun transition(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        now: Long,
        context: OrderRulesContext = OrderRulesContext()
    ): OrderTransitionResult {
        if (order.status in setOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED)) {
            return OrderTransitionResult.Failure(OrderActionBlockReason.TerminalStatus(order.status))
        }

        return when (order.status) {
            OrderStatus.STAFFING -> transitionFromStaffing(order, event, actor, context)
            OrderStatus.IN_PROGRESS -> transitionFromInProgress(order, event, actor, context)
            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> OrderTransitionResult.Failure(OrderActionBlockReason.TerminalStatus(order.status))
        }
    }

    private fun loaderActionsFor(order: Order, actor: CurrentUser, context: OrderRulesContext): OrderActions {
        val alreadyApplied = order.applications.any {
            it.loaderId == actor.id && it.status == OrderApplicationStatus.APPLIED
        }
        val alreadySelected = order.applications.any {
            it.loaderId == actor.id && it.status == OrderApplicationStatus.SELECTED
        }
        val hasActiveApplication = alreadyApplied || alreadySelected

        val applyReason = when {
            order.status != OrderStatus.STAFFING -> OrderActionBlockReason.ActionAllowedOnlyInStatus(OrderStatus.STAFFING)
            context.activeAssignmentExists -> OrderActionBlockReason.ActiveAssignmentExists
            context.activeApplicationsForLimitCount >= MAX_ACTIVE_APPLICATIONS -> OrderActionBlockReason.ApplyLimitReached
            alreadyApplied -> OrderActionBlockReason.AlreadyApplied
            alreadySelected -> OrderActionBlockReason.AlreadySelected
            else -> null
        }

        val withdrawReason = when {
            order.status != OrderStatus.STAFFING -> OrderActionBlockReason.ActionAllowedOnlyInStatus(OrderStatus.STAFFING)
            !hasActiveApplication -> OrderActionBlockReason.NoActiveApplicationToWithdraw
            else -> null
        }

        val completeReason = when {
            order.status != OrderStatus.IN_PROGRESS -> OrderActionBlockReason.ActionAllowedOnlyInStatus(OrderStatus.IN_PROGRESS)
            !context.loaderHasActiveAssignmentInThisOrder -> OrderActionBlockReason.LoaderNotAssignedToOrder
            else -> null
        }

        return OrderActions(
            canApply = applyReason == null,
            applyDisabledReason = applyReason,
            canWithdraw = withdrawReason == null,
            withdrawDisabledReason = withdrawReason,
            canComplete = completeReason == null,
            completeDisabledReason = completeReason,
            canOpenChat = order.status == OrderStatus.IN_PROGRESS && context.loaderHasActiveAssignmentInThisOrder
        )
    }

    private fun dispatcherActionsFor(order: Order, actor: CurrentUser): OrderActions {
        val isCreator = actor.id == order.createdByUserId
        return when (order.status) {
            OrderStatus.STAFFING -> {
                val selectedCount = order.applications.count { it.status == OrderApplicationStatus.SELECTED }
                val startReason = when {
                    !isCreator -> OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing
                    selectedCount != order.workersTotal -> OrderActionBlockReason.SelectedCountMismatch(
                        selected = selectedCount,
                        required = order.workersTotal
                    )
                    else -> null
                }
                val cancelReason = if (!isCreator) {
                    OrderActionBlockReason.OnlyDispatcherCreatorCanCancel
                } else {
                    null
                }
                OrderActions(
                    canSelect = isCreator,
                    canUnselect = isCreator,
                    canStart = startReason == null,
                    startDisabledReason = startReason,
                    canCancel = cancelReason == null,
                    cancelDisabledReason = cancelReason
                )
            }

            OrderStatus.IN_PROGRESS -> {
                val cancelReason = if (!isCreator) OrderActionBlockReason.OnlyDispatcherCreatorCanCancel else null
                val completeReason = if (!isCreator) {
                    OrderActionBlockReason.OnlyDispatcherCreatorOrAssignedLoaderCanComplete
                } else {
                    null
                }
                OrderActions(
                    canCancel = cancelReason == null,
                    cancelDisabledReason = cancelReason,
                    canComplete = completeReason == null,
                    completeDisabledReason = completeReason,
                    canOpenChat = true
                )
            }

            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> OrderActions()
        }
    }

    private fun transitionFromStaffing(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        context: OrderRulesContext
    ): OrderTransitionResult = when (event) {
        OrderEvent.APPLY -> {
            if (actor.role != Role.LOADER) {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyLoaderCanApply)
            } else {
                val reason = loaderActionsFor(order, actor, context).applyDisabledReason
                if (reason == null) OrderTransitionResult.Success(order) else OrderTransitionResult.Failure(reason)
            }
        }

        OrderEvent.WITHDRAW -> {
            if (actor.role != Role.LOADER) {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyLoaderCanWithdraw)
            } else {
                val reason = loaderActionsFor(order, actor, context).withdrawDisabledReason
                if (reason == null) OrderTransitionResult.Success(order) else OrderTransitionResult.Failure(reason)
            }
        }

        OrderEvent.SELECT -> {
            if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing)
            } else {
                OrderTransitionResult.Success(order)
            }
        }

        OrderEvent.UNSELECT -> {
            if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing)
            } else {
                OrderTransitionResult.Success(order)
            }
        }

        OrderEvent.START -> {
            when {
                actor.role != Role.DISPATCHER -> OrderTransitionResult.Failure(OrderActionBlockReason.OnlyDispatcherCanStart)
                actor.id != order.createdByUserId -> OrderTransitionResult.Failure(
                    OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing
                )
                else -> {
                    val reason = dispatcherActionsFor(order, actor).startDisabledReason
                    if (reason == null) {
                        OrderTransitionResult.Success(order.copy(status = OrderStatus.IN_PROGRESS))
                    } else {
                        OrderTransitionResult.Failure(reason)
                    }
                }
            }
        }

        OrderEvent.CANCEL -> {
            if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyDispatcherCreatorCanCancel)
            } else {
                OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
            }
        }

        OrderEvent.EXPIRE -> OrderTransitionResult.Success(order.copy(status = OrderStatus.EXPIRED))
        OrderEvent.COMPLETE -> OrderTransitionResult.Failure(
            OrderActionBlockReason.UnsupportedEventForStatus(event, order.status)
        )
    }

    private fun transitionFromInProgress(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        context: OrderRulesContext
    ): OrderTransitionResult = when (event) {
        OrderEvent.COMPLETE -> {
            val allowed = when (actor.role) {
                Role.DISPATCHER -> actor.id == order.createdByUserId
                Role.LOADER -> context.loaderHasActiveAssignmentInThisOrder
            }
            if (allowed) {
                OrderTransitionResult.Success(order.copy(status = OrderStatus.COMPLETED))
            } else {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyDispatcherCreatorOrAssignedLoaderCanComplete)
            }
        }

        OrderEvent.CANCEL -> {
            if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure(OrderActionBlockReason.OnlyDispatcherCreatorCanCancel)
            } else {
                OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
            }
        }

        OrderEvent.APPLY,
        OrderEvent.WITHDRAW,
        OrderEvent.SELECT,
        OrderEvent.UNSELECT,
        OrderEvent.START,
        OrderEvent.EXPIRE -> OrderTransitionResult.Failure(
            OrderActionBlockReason.UnsupportedEventForStatus(event, order.status)
        )
    }
}
