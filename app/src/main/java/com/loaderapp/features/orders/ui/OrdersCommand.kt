package com.loaderapp.features.orders.ui

import com.loaderapp.features.orders.domain.OrderDraft

sealed class OrdersCommand {
    data object Refresh : OrdersCommand()
    data class Create(val orderDraft: OrderDraft) : OrdersCommand()

    // ── New staffing flow ────────────────────────────────────────────────────
    /** Грузчик откликается на заказ. */
    data class Apply(val orderId: Long) : OrdersCommand()

    /** Грузчик отзывает отклик. */
    data class Withdraw(val orderId: Long) : OrdersCommand()

    /** Диспетчер-создатель выбирает грузчика из откликов. */
    data class Select(val orderId: Long, val loaderId: String) : OrdersCommand()

    /** Диспетчер-создатель снимает выбор грузчика. */
    data class Unselect(val orderId: Long, val loaderId: String) : OrdersCommand()

    /** Диспетчер-создатель запускает заказ (STAFFING → IN_PROGRESS). */
    data class Start(val orderId: Long) : OrdersCommand()

    // ── Common lifecycle ─────────────────────────────────────────────────────
    data class Cancel(val orderId: Long, val reason: String?) : OrdersCommand()
    data class Complete(val orderId: Long) : OrdersCommand()

    // ── Deprecated compat ────────────────────────────────────────────────────
    /**
     * @Deprecated Используйте [Apply].
     * Оставлено только для компиляции call-site'ов до UI-рефакторинга (Step 5).
     */
    @Deprecated(
        message = "Use OrdersCommand.Apply. Will be removed in Step 5.",
        replaceWith = ReplaceWith("Apply(orderId)")
    )
    data class Accept(val orderId: Long) : OrdersCommand()
}
