package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser
import javax.inject.Inject

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

data class OrdersLimits(val maxActiveApplications: Int = 3)

class OrderStateMachine @Inject constructor(
    private val limits: OrdersLimits
) {

    fun actionsFor(
        order: Order,
        actor: CurrentUser,
        context: OrderRulesContext = OrderRulesContext()
    ): OrderActions = when (actor.role) {
        Role.LOADER -> {
            val apply = decisionFor(order, OrderEvent.APPLY, actor, context)
            val withdraw = decisionFor(order, OrderEvent.WITHDRAW, actor, context)
            val complete = decisionFor(order, OrderEvent.COMPLETE, actor, context)
            OrderActions(
                canApply = apply.isAllowed,
                applyDisabledReason = apply.reason,
                canWithdraw = withdraw.isAllowed,
                withdrawDisabledReason = withdraw.reason,
                canComplete = complete.isAllowed,
                completeDisabledReason = complete.reason,
                canOpenChat = canOpenChat(order, actor, context)
            )
        }

        Role.DISPATCHER -> when (order.status) {
            OrderStatus.STAFFING -> {
                val select = decisionFor(order, OrderEvent.SELECT, actor, context)
                val unselect = decisionFor(order, OrderEvent.UNSELECT, actor, context)
                val start = decisionFor(order, OrderEvent.START, actor, context)
                val cancel = decisionFor(order, OrderEvent.CANCEL, actor, context)
                OrderActions(
                    canSelect = select.isAllowed,
                    canUnselect = unselect.isAllowed,
                    canStart = start.isAllowed,
                    startDisabledReason = start.reason,
                    canCancel = cancel.isAllowed,
                    cancelDisabledReason = cancel.reason
                )
            }

            OrderStatus.IN_PROGRESS -> {
                val cancel = decisionFor(order, OrderEvent.CANCEL, actor, context)
                val complete = decisionFor(order, OrderEvent.COMPLETE, actor, context)
                OrderActions(
                    canCancel = cancel.isAllowed,
                    cancelDisabledReason = cancel.reason,
                    canComplete = complete.isAllowed,
                    completeDisabledReason = complete.reason,
                    canOpenChat = true
                )
            }

            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> OrderActions()
        }
    }

    fun transition(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        now: Long,
        context: OrderRulesContext = OrderRulesContext()
    ): OrderTransitionResult {
        val decision = decisionFor(order, event, actor, context)
        if (!decision.isAllowed) {
            return OrderTransitionResult.Failure(decision.reason!!)
        }

        val nextOrder = when (event) {
            OrderEvent.START -> order.copy(status = OrderStatus.IN_PROGRESS)
            OrderEvent.CANCEL -> order.copy(status = OrderStatus.CANCELED)
            OrderEvent.COMPLETE -> order.copy(status = OrderStatus.COMPLETED)
            OrderEvent.EXPIRE -> order.copy(status = OrderStatus.EXPIRED)
            OrderEvent.APPLY,
            OrderEvent.WITHDRAW,
            OrderEvent.SELECT,
            OrderEvent.UNSELECT -> order
        }
        return OrderTransitionResult.Success(nextOrder)
    }

    private fun decisionFor(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        context: OrderRulesContext
    ): Decision {
        if (order.status in setOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED)) {
            return Decision.denied(OrderActionBlockReason.TerminalStatus(order.status))
        }

        return when (event) {
            OrderEvent.APPLY -> canApply(order, actor, context)
            OrderEvent.WITHDRAW -> canWithdraw(order, actor, context)
            OrderEvent.SELECT -> canSelect(order, actor)
            OrderEvent.UNSELECT -> canUnselect(order, actor)
            OrderEvent.START -> canStart(order, actor)
            OrderEvent.CANCEL -> canCancel(order, actor)
            OrderEvent.COMPLETE -> canComplete(order, actor, context)
            OrderEvent.EXPIRE -> canExpire(order)
        }
    }

    private fun canApply(order: Order, actor: CurrentUser, context: OrderRulesContext): Decision {
        if (order.status != OrderStatus.STAFFING) {
            return Decision.denied(OrderActionBlockReason.ActionAllowedOnlyInStatus(OrderStatus.STAFFING))
        }
        if (actor.role != Role.LOADER) return Decision.denied(OrderActionBlockReason.OnlyLoaderCanApply)

        val alreadyApplied = order.applications.any {
            it.loaderId == actor.id && it.status == OrderApplicationStatus.APPLIED
        }
        val alreadySelected = order.applications.any {
            it.loaderId == actor.id && it.status == OrderApplicationStatus.SELECTED
        }

        val reason = when {
            context.loaderHasActiveAssignmentInThisOrder -> OrderActionBlockReason.AlreadyAssignedToOrder
            context.activeAssignmentExists -> OrderActionBlockReason.ActiveAssignmentExists
            context.activeApplicationsForLimitCount >= limits.maxActiveApplications -> OrderActionBlockReason.ApplyLimitReached
            alreadyApplied -> OrderActionBlockReason.AlreadyApplied
            alreadySelected -> OrderActionBlockReason.AlreadySelected
            else -> null
        }
        return if (reason == null) Decision.allowed() else Decision.denied(reason)
    }

    private fun canWithdraw(order: Order, actor: CurrentUser, context: OrderRulesContext): Decision {
        if (order.status != OrderStatus.STAFFING) {
            return Decision.denied(OrderActionBlockReason.ActionAllowedOnlyInStatus(OrderStatus.STAFFING))
        }
        if (actor.role != Role.LOADER) return Decision.denied(OrderActionBlockReason.OnlyLoaderCanWithdraw)

        val hasActiveApplication = order.applications.any {
            it.loaderId == actor.id && it.status in setOf(OrderApplicationStatus.APPLIED, OrderApplicationStatus.SELECTED)
        }

        return if (hasActiveApplication) Decision.allowed()
        else Decision.denied(OrderActionBlockReason.NoActiveApplicationToWithdraw)
    }

    private fun canSelect(order: Order, actor: CurrentUser): Decision {
        if (order.status != OrderStatus.STAFFING) {
            return Decision.denied(OrderActionBlockReason.UnsupportedEventForStatus(OrderEvent.SELECT, order.status))
        }
        if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
            return Decision.denied(OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing)
        }
        return Decision.allowed()
    }

    private fun canUnselect(order: Order, actor: CurrentUser): Decision {
        if (order.status != OrderStatus.STAFFING) {
            return Decision.denied(OrderActionBlockReason.UnsupportedEventForStatus(OrderEvent.UNSELECT, order.status))
        }
        if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
            return Decision.denied(OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing)
        }
        return Decision.allowed()
    }

    private fun canStart(order: Order, actor: CurrentUser): Decision {
        if (order.status != OrderStatus.STAFFING) {
            return Decision.denied(OrderActionBlockReason.UnsupportedEventForStatus(OrderEvent.START, order.status))
        }
        if (actor.role != Role.DISPATCHER) return Decision.denied(OrderActionBlockReason.OnlyDispatcherCanStart)
        if (actor.id != order.createdByUserId) {
            return Decision.denied(OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing)
        }

        val selectedCount = order.applications.count { it.status == OrderApplicationStatus.SELECTED }
        return if (selectedCount == order.workersTotal) {
            Decision.allowed()
        } else {
            Decision.denied(OrderActionBlockReason.SelectedCountMismatch(selectedCount, order.workersTotal))
        }
    }

    private fun canCancel(order: Order, actor: CurrentUser): Decision {
        if (order.status !in setOf(OrderStatus.STAFFING, OrderStatus.IN_PROGRESS)) {
            return Decision.denied(OrderActionBlockReason.UnsupportedEventForStatus(OrderEvent.CANCEL, order.status))
        }
        if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
            return Decision.denied(OrderActionBlockReason.OnlyDispatcherCreatorCanCancel)
        }
        return Decision.allowed()
    }

    private fun canComplete(order: Order, actor: CurrentUser, context: OrderRulesContext): Decision {
        if (order.status != OrderStatus.IN_PROGRESS) {
            return Decision.denied(OrderActionBlockReason.ActionAllowedOnlyInStatus(OrderStatus.IN_PROGRESS))
        }
        val allowed = when (actor.role) {
            Role.DISPATCHER -> actor.id == order.createdByUserId
            Role.LOADER -> context.loaderHasActiveAssignmentInThisOrder
        }
        return if (allowed) Decision.allowed()
        else Decision.denied(OrderActionBlockReason.OnlyDispatcherCreatorOrAssignedLoaderCanComplete)
    }

    private fun canExpire(order: Order): Decision {
        return if (order.status == OrderStatus.STAFFING) Decision.allowed()
        else Decision.denied(OrderActionBlockReason.UnsupportedEventForStatus(OrderEvent.EXPIRE, order.status))
    }

    private fun canOpenChat(order: Order, actor: CurrentUser, context: OrderRulesContext): Boolean =
        when (actor.role) {
            Role.DISPATCHER -> order.status == OrderStatus.IN_PROGRESS && actor.id == order.createdByUserId
            Role.LOADER -> order.status == OrderStatus.IN_PROGRESS && context.loaderHasActiveAssignmentInThisOrder
        }

    private data class Decision(
        val isAllowed: Boolean,
        val reason: OrderActionBlockReason?
    ) {
        companion object {
            fun allowed() = Decision(true, null)
            fun denied(reason: OrderActionBlockReason) = Decision(false, reason)
        }
    }
}
