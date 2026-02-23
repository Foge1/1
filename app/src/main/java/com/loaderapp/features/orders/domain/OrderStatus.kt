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
}
