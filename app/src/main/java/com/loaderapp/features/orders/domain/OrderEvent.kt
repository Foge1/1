package com.loaderapp.features.orders.domain

enum class OrderEvent {
    /** Грузчик откликается на заказ. */
    APPLY,

    /** Грузчик отзывает отклик. */
    WITHDRAW,

    /** Диспетчер выбирает грузчика из откликов. */
    SELECT,

    /** Диспетчер снимает выбор грузчика. */
    UNSELECT,

    /** Диспетчер стартует заказ (STAFFING → IN_PROGRESS). */
    START,

    CANCEL,
    COMPLETE,
    EXPIRE,

    /**
     * @Deprecated Use [APPLY] + [SELECT] + [START].
     * Kept only so existing call-sites compile during migration.
     */
    @Deprecated("Use APPLY/SELECT/START flow. Will be removed.", ReplaceWith("APPLY"))
    ACCEPT
}
