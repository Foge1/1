package com.loaderapp.features.orders.domain

enum class OrderStatus {
    /**
     * Order is open for applications (грузчики могут откликаться).
     */
    STAFFING,
    IN_PROGRESS,
    COMPLETED,
    CANCELED,
    EXPIRED

    ;

    companion object {
        val ACTIVE_FOR_APPLICATION_LIMIT: Set<OrderStatus> = setOf(STAFFING, IN_PROGRESS)
    }
}
