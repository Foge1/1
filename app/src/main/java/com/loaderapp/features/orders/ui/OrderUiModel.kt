package com.loaderapp.features.orders.ui

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role

/**
 * UI-представление заказа с заранее вычисленными флагами действий.
 *
 * Все [can*] поля и [*DisabledReason] вычислены единожды в
 * [ObserveOrderUiModelsUseCase] через [OrderStateMachine.actionsFor] —
 * composable-функции не принимают решений о доступности сами.
 *
 * [currentUserId] хранится здесь, чтобы computed-свойства
 * ([myApplication], [myApplicationStatus]) корректно привязывались к актору,
 * а не к первой попавшейся заявке в списке.
 */
data class OrderUiModel(
    val order: Order,

    /** Id актора, для которого вычислена эта модель. */
    val currentUserId: String,
    val currentUserRole: Role,

    // ── Loader actions ────────────────────────────────────────────────────────
    val canApply: Boolean,
    val applyDisabledReason: String?,
    val canWithdraw: Boolean,
    val withdrawDisabledReason: String?,

    // ── Dispatcher actions ────────────────────────────────────────────────────
    /** true только у диспетчера-создателя в статусе STAFFING. */
    val canSelect: Boolean,
    /** true только у диспетчера-создателя в статусе STAFFING. */
    val canUnselect: Boolean,
    /**
     * true когда создатель и selectedApplicantsCount == order.workersTotal.
     */
    val canStart: Boolean,
    val startDisabledReason: String?,

    // ── Common lifecycle ──────────────────────────────────────────────────────
    val canCancel: Boolean,
    val cancelDisabledReason: String?,
    val canComplete: Boolean,
    val completeDisabledReason: String?,
    val canOpenChat: Boolean,
) {
    // ── Computed: actor's own application ─────────────────────────────────────

    /**
     * Заявка текущего пользователя на этот заказ.
     * Null если актор — диспетчер или у него нет заявки.
     */
    val myApplication: OrderApplication?
        get() = order.applications.firstOrNull { it.loaderId == currentUserId }

    /**
     * Статус заявки текущего пользователя.
     * Null если нет заявки или актор — диспетчер.
     */
    val myApplicationStatus: OrderApplicationStatus?
        get() = myApplication?.status

    // ── Computed: applicant counters (для UI диспетчера) ─────────────────────

    val selectedApplicantsCount: Int
        get() = order.applications.count { it.status == OrderApplicationStatus.SELECTED }

    val appliedApplicantsCount: Int
        get() = order.applications.count { it.status == OrderApplicationStatus.APPLIED }

    /**
     * Список откликов, видимых в UI диспетчера.
     * Показываем только APPLIED + SELECTED; WITHDRAWN/REJECTED скрыты.
     */
    val visibleApplicants: List<OrderApplication>
        get() = order.applications.filter {
            it.status == OrderApplicationStatus.APPLIED ||
                it.status == OrderApplicationStatus.SELECTED
        }
}

// ── Legacy bridge ─────────────────────────────────────────────────────────────
// Нужен для передачи данных в старые composable-компоненты (OrderCard).
// Не содержит бизнес-логики.

fun OrderModel.toFeatureStatus(): OrderStatus = when (status) {
    OrderStatusModel.AVAILABLE -> OrderStatus.STAFFING
    OrderStatusModel.TAKEN, OrderStatusModel.IN_PROGRESS -> OrderStatus.IN_PROGRESS
    OrderStatusModel.COMPLETED -> OrderStatus.COMPLETED
    OrderStatusModel.CANCELLED -> OrderStatus.CANCELED
}

fun Order.toLegacyOrderModel(): OrderModel = toOrderModel()

fun Order.toOrderModel(): OrderModel {
    val durationHours = (durationMin / 60).coerceAtLeast(1)
    return OrderModel(
        id = id,
        address = address,
        dateTime = dateTime,
        cargoDescription = tags.firstOrNull() ?: title,
        pricePerHour = pricePerHour,
        estimatedHours = durationHours,
        requiredWorkers = workersTotal,
        minWorkerRating = meta[MIN_WORKER_RATING_KEY]?.toFloatOrNull() ?: 0f,
        status = status.toLegacyStatusModel(),
        createdAt = meta[Order.CREATED_AT_KEY]?.toLongOrNull() ?: dateTime,
        completedAt = null,
        workerId = if (workersCurrent > 0) 1L else null,
        dispatcherId = meta[DISPATCHER_ID_KEY]?.toLongOrNull() ?: 0L,
        workerRating = null,
        comment = comment.orEmpty()
    )
}

fun OrderUiModel.toLegacyOrderModel(): OrderModel = order.toLegacyOrderModel()

@Suppress("DEPRECATION")
private fun OrderStatus.toLegacyStatusModel(): OrderStatusModel = when (this) {
    OrderStatus.STAFFING,
    OrderStatus.AVAILABLE -> OrderStatusModel.AVAILABLE
    OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
    OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
    OrderStatus.CANCELED,
    OrderStatus.EXPIRED -> OrderStatusModel.CANCELLED
}

private const val MIN_WORKER_RATING_KEY = "minWorkerRating"
private const val DISPATCHER_ID_KEY = "dispatcherId"
