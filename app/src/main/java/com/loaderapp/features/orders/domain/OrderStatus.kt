package com.loaderapp.features.orders.domain

enum class OrderStatus {
    /**
     * Order is open for applications (грузчики могут откликаться).
     * Replaces the old AVAILABLE status.
     */
    STAFFING,

    /**
     * @Deprecated Use [STAFFING]. Kept for migration compatibility only.
     * Will be removed in a future step.
     */
    @Deprecated("Use STAFFING. This value exists only for DB migration compatibility.", ReplaceWith("STAFFING"))
    AVAILABLE,

    IN_PROGRESS,
    COMPLETED,
    CANCELED,
    EXPIRED
}
