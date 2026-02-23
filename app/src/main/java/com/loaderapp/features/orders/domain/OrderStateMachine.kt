package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser

/**
 * Контекст, необходимый для вычисления доступных действий.
 *
 * @param activeAssignmentExists                 true если у актора уже есть ACTIVE assignment в другом заказе.
 * @param activeAppliedCount                     количество откликов со статусом APPLIED у актора по всем заказам.
 * @param loaderHasActiveAssignmentInThisOrder   true если у актора есть ACTIVE assignment именно в этом заказе.
 */
data class OrderRulesContext(
    val activeAssignmentExists: Boolean = false,
    val activeAppliedCount: Int = 0,
    val loaderHasActiveAssignmentInThisOrder: Boolean = false
)

/**
 * Набор доступных действий + причины недоступности.
 *
 * canXxx == true  →  кнопка активна.
 * xxxDisabledReason != null  →  действие заблокировано (текст для UI).
 */
data class OrderActions(
    // Loader
    val canApply: Boolean = false,
    val applyDisabledReason: String? = null,
    val canWithdraw: Boolean = false,
    val withdrawDisabledReason: String? = null,

    // Dispatcher
    val canSelect: Boolean = false,
    val canUnselect: Boolean = false,
    val canStart: Boolean = false,
    val startDisabledReason: String? = null,
    val canCancel: Boolean = false,
    val cancelDisabledReason: String? = null,
    val canComplete: Boolean = false,
    val completeDisabledReason: String? = null,

    val canOpenChat: Boolean = false
)

/** Максимальное количество одновременных активных откликов грузчика. */
private const val MAX_ACTIVE_APPLICATIONS = 3

object OrderStateMachine {

    // ─── Public API ──────────────────────────────────────────────────────────────

    /**
     * Возвращает набор действий, доступных [actor]-у для данного [order].
     * [context] содержит данные о состоянии грузчика за пределами этого заказа.
     */
    fun actionsFor(
        order: Order,
        actor: CurrentUser,
        context: OrderRulesContext = OrderRulesContext()
    ): OrderActions = when (actor.role) {
        Role.LOADER -> loaderActionsFor(order, actor, context)
        Role.DISPATCHER -> dispatcherActionsFor(order, actor)
    }

    /**
     * Валидирует переход и возвращает Success/Failure.
     * НЕ изменяет репозиторий/БД.
     *
     * APPLY / WITHDRAW / SELECT / UNSELECT не меняют order.status —
     * фактические изменения application-записей выполняются в UseCase (Step 4+).
     */
    fun transition(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        now: Long,
        context: OrderRulesContext = OrderRulesContext()
    ): OrderTransitionResult = when (order.status) {
        OrderStatus.STAFFING -> transitionFromStaffing(order, event, actor, now, context)
        OrderStatus.IN_PROGRESS -> transitionFromInProgress(order, event, actor, context)
        OrderStatus.COMPLETED,
        OrderStatus.CANCELED,
        OrderStatus.EXPIRED -> OrderTransitionResult.Failure(
            "Переход невозможен: заказ в терминальном статусе ${order.status}"
        )
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    private fun loaderActionsFor(
        order: Order,
        actor: CurrentUser,
        context: OrderRulesContext
    ): OrderActions {
        val alreadyApplied = order.applications.any {
            it.loaderId == actor.id && it.status == OrderApplicationStatus.APPLIED
        }
        val alreadySelected = order.applications.any {
            it.loaderId == actor.id && it.status == OrderApplicationStatus.SELECTED
        }
        val hasActiveApplication = alreadyApplied || alreadySelected

        val applyDisabledReason: String? = when {
            order.status != OrderStatus.STAFFING ->
                "Заказ не принимает отклики (статус: ${order.status})"
            context.activeAssignmentExists ->
                "У вас уже есть активный заказ"
            context.activeAppliedCount >= MAX_ACTIVE_APPLICATIONS ->
                "Достигнут лимит откликов ($MAX_ACTIVE_APPLICATIONS)"
            alreadyApplied -> "Вы уже откликнулись на этот заказ"
            alreadySelected -> "Вы уже выбраны в этот заказ"
            else -> null
        }

        val withdrawDisabledReason: String? = when {
            order.status != OrderStatus.STAFFING -> "Заказ не в статусе набора"
            !hasActiveApplication -> "У вас нет активного отклика на этот заказ"
            else -> null
        }

        val isInProgress = order.status == OrderStatus.IN_PROGRESS
        val canComplete = isInProgress && context.loaderHasActiveAssignmentInThisOrder
        val completeDisabledReason: String? = when {
            !isInProgress -> "Заказ не выполняется"
            !context.loaderHasActiveAssignmentInThisOrder -> "Вы не назначены на этот заказ"
            else -> null
        }

        return OrderActions(
            canApply = applyDisabledReason == null,
            applyDisabledReason = applyDisabledReason,
            canWithdraw = withdrawDisabledReason == null,
            withdrawDisabledReason = withdrawDisabledReason,
            canComplete = canComplete,
            completeDisabledReason = completeDisabledReason,
            canOpenChat = isInProgress && context.loaderHasActiveAssignmentInThisOrder
        )
    }

    private fun dispatcherActionsFor(
        order: Order,
        actor: CurrentUser
    ): OrderActions {
        val isCreator = actor.id == order.createdByUserId

        return when (order.status) {
            OrderStatus.STAFFING -> {
                val selectedCount = order.applications.count {
                    it.status == OrderApplicationStatus.SELECTED
                }
                val startDisabledReason: String? = when {
                    !isCreator -> "Только создатель заказа может его запустить"
                    selectedCount != order.workersTotal ->
                        "Выбрано $selectedCount из ${order.workersTotal} грузчиков"
                    else -> null
                }
                val cancelDisabledReason: String? =
                    if (!isCreator) "Только создатель заказа может его отменить" else null

                OrderActions(
                    canSelect = isCreator,
                    canUnselect = isCreator,
                    canStart = startDisabledReason == null,
                    startDisabledReason = startDisabledReason,
                    canCancel = cancelDisabledReason == null,
                    cancelDisabledReason = cancelDisabledReason
                )
            }

            OrderStatus.IN_PROGRESS -> {
                val cancelDisabledReason: String? =
                    if (!isCreator) "Только создатель заказа может его отменить" else null
                val completeDisabledReason: String? =
                    if (!isCreator) "Только создатель заказа может завершить его" else null

                OrderActions(
                    canCancel = cancelDisabledReason == null,
                    cancelDisabledReason = cancelDisabledReason,
                    canComplete = completeDisabledReason == null,
                    completeDisabledReason = completeDisabledReason,
                    canOpenChat = true
                )
            }

            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> OrderActions()
        }
    }

    // ─── Transitions ─────────────────────────────────────────────────────────────

    private fun transitionFromStaffing(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        now: Long,
        context: OrderRulesContext
    ): OrderTransitionResult = when (event) {

        OrderEvent.APPLY -> {
            when {
                actor.role != Role.LOADER ->
                    OrderTransitionResult.Failure("Только грузчик может откликнуться на заказ")
                context.activeAssignmentExists ->
                    OrderTransitionResult.Failure("У грузчика уже есть активный заказ")
                context.activeAppliedCount >= MAX_ACTIVE_APPLICATIONS ->
                    OrderTransitionResult.Failure(
                        "Достигнут лимит активных откликов ($MAX_ACTIVE_APPLICATIONS)"
                    )
                else -> OrderTransitionResult.Success(order) // статус не меняется; applications — в UseCase
            }
        }

        OrderEvent.WITHDRAW -> {
            if (actor.role != Role.LOADER) {
                OrderTransitionResult.Failure("Только грузчик может отозвать отклик")
            } else {
                OrderTransitionResult.Success(order) // статус не меняется; applications — в UseCase
            }
        }

        OrderEvent.SELECT -> {
            if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure("Только диспетчер-создатель может выбирать грузчиков")
            } else {
                OrderTransitionResult.Success(order)
            }
        }

        OrderEvent.UNSELECT -> {
            if (actor.role != Role.DISPATCHER || actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure("Только диспетчер-создатель может снимать выбор")
            } else {
                OrderTransitionResult.Success(order)
            }
        }

        OrderEvent.START -> {
            when {
                actor.role != Role.DISPATCHER ->
                    OrderTransitionResult.Failure("Только диспетчер может запустить заказ")
                actor.id != order.createdByUserId ->
                    OrderTransitionResult.Failure("Только диспетчер-создатель может запустить заказ")
                else -> {
                    val selectedCount = order.applications.count {
                        it.status == OrderApplicationStatus.SELECTED
                    }
                    if (selectedCount != order.workersTotal) {
                        OrderTransitionResult.Failure(
                            "Невозможно запустить: выбрано $selectedCount из ${order.workersTotal} грузчиков"
                        )
                    } else {
                        OrderTransitionResult.Success(order.copy(status = OrderStatus.IN_PROGRESS))
                    }
                }
            }
        }

        OrderEvent.CANCEL -> {
            if (actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure("Только диспетчер-создатель может отменить заказ")
            } else {
                OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
            }
        }

        OrderEvent.EXPIRE ->
            OrderTransitionResult.Success(order.copy(status = OrderStatus.EXPIRED))

        OrderEvent.COMPLETE ->
            OrderTransitionResult.Failure("Нельзя завершить заказ из статуса ${order.status}")

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
                OrderTransitionResult.Failure(
                    "Завершить заказ может только диспетчер-создатель или грузчик с активным назначением"
                )
            }
        }

        OrderEvent.CANCEL -> {
            if (actor.id != order.createdByUserId) {
                OrderTransitionResult.Failure(
                    "Только диспетчер-создатель может отменить заказ в процессе выполнения"
                )
            } else {
                OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
            }
        }

        OrderEvent.APPLY ->
            OrderTransitionResult.Failure("Нельзя откликнуться: заказ в статусе ${order.status}")

        OrderEvent.WITHDRAW ->
            OrderTransitionResult.Failure("Нельзя отозвать отклик: заказ в статусе ${order.status}")

        OrderEvent.SELECT ->
            OrderTransitionResult.Failure("Нельзя выбрать грузчика: заказ в статусе ${order.status}")

        OrderEvent.UNSELECT ->
            OrderTransitionResult.Failure("Нельзя снять выбор: заказ в статусе ${order.status}")

        OrderEvent.START ->
            OrderTransitionResult.Failure("Заказ уже запущен")

        OrderEvent.EXPIRE ->
            OrderTransitionResult.Failure("Нельзя истечь заказ в статусе ${order.status}")

    }
}
