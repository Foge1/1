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
    EXPIRE
}
