package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser

object OrderStateMachine {
    data class OrderActions(
        val canApply: Boolean,
        val canWithdraw: Boolean,
        val canSelect: Boolean,
        val canUnselect: Boolean,
        val canStart: Boolean,
        val canCancel: Boolean,
        val canComplete: Boolean,
        val canOpenChat: Boolean,
        val applyDisabledReason: String?
    )

    fun actionsFor(
        order: Order,
        actor: CurrentUser,
        context: OrderRulesContext
    ): OrderActions {
        val canApply = canApply(order, actor, context)
        return OrderActions(
            canApply = canApply,
            canWithdraw = canWithdraw(order, actor),
            canSelect = canSelect(order, actor),
            canUnselect = canUnselect(order, actor),
            canStart = canStart(order, actor),
            canCancel = canCancel(order, actor),
            canComplete = canComplete(order, actor),
            canOpenChat = canOpenChat(order, actor),
            applyDisabledReason = if (canApply) null else applyDisabledReason(order, actor, context)
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun transition(order: Order, event: OrderEvent, actor: CurrentUser, now: Long): OrderTransitionResult {
        return when (order.status) {
            OrderStatus.STAFFING -> transitionFromStaffing(order, event, actor)
            OrderStatus.IN_PROGRESS -> transitionFromInProgress(order, event, actor)
            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> {
                OrderTransitionResult.Failure("Переходы из терминального статуса ${order.status} недоступны")
            }
        }
    }

    private fun transitionFromStaffing(order: Order, event: OrderEvent, actor: CurrentUser): OrderTransitionResult {
        return when (event) {
            OrderEvent.START -> {
                if (!canStart(order, actor)) {
                    OrderTransitionResult.Failure("Запуск заказа недоступен")
                } else {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.IN_PROGRESS))
                }
            }

            OrderEvent.CANCEL -> {
                if (!canCancel(order, actor)) {
                    OrderTransitionResult.Failure("Отмена заказа недоступна")
                } else {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
                }
            }

            OrderEvent.EXPIRE -> OrderTransitionResult.Success(order.copy(status = OrderStatus.EXPIRED))
            OrderEvent.APPLY,
            OrderEvent.WITHDRAW,
            OrderEvent.SELECT,
            OrderEvent.UNSELECT,
            OrderEvent.COMPLETE -> OrderTransitionResult.Failure(
                "Событие $event не меняет статус заказа из ${order.status} на шаге 1"
            )
        }
    }

    private fun transitionFromInProgress(order: Order, event: OrderEvent, actor: CurrentUser): OrderTransitionResult {
        return when (event) {
            OrderEvent.CANCEL -> {
                if (!canCancel(order, actor)) {
                    OrderTransitionResult.Failure("Отмена заказа недоступна")
                } else {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
                }
            }

            OrderEvent.COMPLETE -> {
                if (!canComplete(order, actor)) {
                    OrderTransitionResult.Failure("Завершение заказа недоступно")
                } else {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.COMPLETED))
                }
            }

            OrderEvent.APPLY,
            OrderEvent.WITHDRAW,
            OrderEvent.SELECT,
            OrderEvent.UNSELECT,
            OrderEvent.START,
            OrderEvent.EXPIRE -> OrderTransitionResult.Failure(
                "Событие $event недоступно из статуса ${order.status}"
            )
        }
    }

    private fun canApply(order: Order, actor: CurrentUser, context: OrderRulesContext): Boolean {
        return applyDisabledReason(order, actor, context) == null
    }

    private fun applyDisabledReason(order: Order, actor: CurrentUser, context: OrderRulesContext): String? {
        if (actor.role != Role.LOADER) return "Только грузчик может откликнуться"
        if (order.status != OrderStatus.STAFFING) return "Отклик доступен только на этапе набора"
        if (context.activeAssignmentExists) return "У вас уже есть активный заказ"
        if (context.activeAppliedCount >= 3) return "Лимит активных откликов: 3"

        val currentApplication = order.applications.lastOrNull { it.loaderId == actor.id }
        return when (currentApplication?.status) {
            OrderApplicationStatus.APPLIED -> "Вы уже откликнулись"
            OrderApplicationStatus.SELECTED -> "Вы уже отобраны"
            OrderApplicationStatus.REJECTED,
            OrderApplicationStatus.WITHDRAWN,
            null -> null
        }
    }

    private fun canWithdraw(order: Order, actor: CurrentUser): Boolean {
        if (actor.role != Role.LOADER || order.status != OrderStatus.STAFFING) return false
        val status = order.applications.lastOrNull { it.loaderId == actor.id }?.status
        return status == OrderApplicationStatus.APPLIED || status == OrderApplicationStatus.SELECTED
    }

    private fun canSelect(order: Order, actor: CurrentUser): Boolean {
        if (!isCreatorDispatcherForStaffing(order, actor)) return false
        val selectedCount = order.applications.count { it.status == OrderApplicationStatus.SELECTED }
        return selectedCount < order.workersTotal
    }

    private fun canUnselect(order: Order, actor: CurrentUser): Boolean {
        return isCreatorDispatcherForStaffing(order, actor)
    }

    private fun canStart(order: Order, actor: CurrentUser): Boolean {
        if (!isCreatorDispatcherForStaffing(order, actor)) return false
        val selectedCount = order.applications.count { it.status == OrderApplicationStatus.SELECTED }
        return selectedCount == order.workersTotal
    }

    private fun canCancel(order: Order, actor: CurrentUser): Boolean {
        if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) return false
        return order.status == OrderStatus.STAFFING || order.status == OrderStatus.IN_PROGRESS
    }

    private fun canComplete(order: Order, actor: CurrentUser): Boolean {
        if (order.status != OrderStatus.IN_PROGRESS) return false
        if (actor.role == Role.DISPATCHER && actor.id == order.createdByUserId) return true
        if (actor.role != Role.LOADER) return false
        return order.assignments.any {
            it.loaderId == actor.id && it.status == OrderAssignmentStatus.ACTIVE
        }
    }

    private fun canOpenChat(order: Order, actor: CurrentUser): Boolean {
        if (order.status != OrderStatus.IN_PROGRESS) return false
        return actor.id == order.createdByUserId || order.assignments.any { it.loaderId == actor.id }
    }

    private fun isCreatorDispatcherForStaffing(order: Order, actor: CurrentUser): Boolean {
        return actor.role == Role.DISPATCHER &&
            actor.id == order.createdByUserId &&
            order.status == OrderStatus.STAFFING
    }
}
