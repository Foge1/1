package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderActions
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.ui.OrderUiModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * Единственная точка вычисления [OrderUiModel] для UI.
 *
 * Алгоритм на каждый снимок заказов:
 * 1. Получаем актора из [CurrentUserProvider].
 * 2. Строим [OrderRulesContext] **одним** `suspend`-запросом к репозиторию (не N запросов).
 * 3. Для каждого заказа вызываем [OrderStateMachine.actionsFor] и создаём [OrderUiModel].
 *
 * Поток перезапускается при смене текущего пользователя ([flatMapLatest]).
 *
 * ── Почему `flow { emit(...) }` вместо `map {}` ──────────────────────────────
 * [buildBaseContext] — suspend-функция. Kotlin запрещает вызывать suspend-функции
 * из лямбды [Flow.map] (она non-inline, non-suspend). Поэтому мы оборачиваем
 * одну асинхронную трансформацию в `flow { emit(...) }` через [flatMapLatest].
 */
class ObserveOrderUiModelsUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val observeOrdersForRoleUseCase: ObserveOrdersForRoleUseCase,
) {
    operator fun invoke(): Flow<List<OrderUiModel>> =
        currentUserProvider.observeCurrentUser()
            .flatMapLatest { actor ->
                // Один suspend-вызов за снимок: строим базовый контекст.
                // Затем подписываемся на поток заказов и трансформируем их синхронно.
                flow {
                    val baseContext = buildBaseContext(actor)
                    // Реэмитим каждый снимок заказов, добавляя вычисленные флаги.
                    observeOrdersForRoleUseCase()
                        .collect { orders ->
                            emit(orders.map { order -> order.toUiModel(actor, baseContext) })
                        }
                }
            }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Строит базовый контекст: один запрос к репозиторию на смену актора.
     * [OrderRulesContext.loaderHasActiveAssignmentInThisOrder] выставляется
     * per-order в [Order.toUiModel] (данные уже в памяти — без IO).
     */
    private suspend fun buildBaseContext(actor: CurrentUser): OrderRulesContext =
        when (actor.role) {
            Role.LOADER -> OrderRulesContext(
                activeAssignmentExists = repository.hasActiveAssignment(actor.id),
                activeAppliedCount = repository.countActiveAppliedApplications(actor.id),
                loaderHasActiveAssignmentInThisOrder = false // переопределяется per-order
            )
            Role.DISPATCHER -> OrderRulesContext()
        }

    private fun Order.toUiModel(actor: CurrentUser, baseContext: OrderRulesContext): OrderUiModel {
        // loaderHasActiveAssignmentInThisOrder — per-order, данные уже в памяти.
        val hasAssignmentHere = actor.role == Role.LOADER &&
            assignments.any { it.loaderId == actor.id && it.status == OrderAssignmentStatus.ACTIVE }

        val context = baseContext.copy(loaderHasActiveAssignmentInThisOrder = hasAssignmentHere)
        val actions: OrderActions = OrderStateMachine.actionsFor(this, actor, context)

        return OrderUiModel(
            order = this,
            currentUserId = actor.id,
            currentUserRole = actor.role,
            // Loader
            canApply = actions.canApply,
            applyDisabledReason = actions.applyDisabledReason,
            canWithdraw = actions.canWithdraw,
            withdrawDisabledReason = actions.withdrawDisabledReason,
            // Dispatcher
            canSelect = actions.canSelect,
            canUnselect = actions.canUnselect,
            canStart = actions.canStart,
            startDisabledReason = actions.startDisabledReason,
            // Common
            canCancel = actions.canCancel,
            cancelDisabledReason = actions.cancelDisabledReason,
            canComplete = actions.canComplete,
            completeDisabledReason = actions.completeDisabledReason,
            canOpenChat = actions.canOpenChat,
        )
    }
}
