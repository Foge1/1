package com.loaderapp.features.orders.domain

/**
 * Represents a loader's application to an order.
 *
 * Lifecycle: APPLIED → SELECTED → (on startOrder) assignment created
 *                   → REJECTED (when order starts and loader was not selected)
 *            APPLIED → WITHDRAWN (loader withdraws before start)
 *            APPLIED / SELECTED → WITHDRAWN (loader withdraws)
 */
data class OrderApplication(
    val orderId: Long,
    val loaderId: String,
    val status: OrderApplicationStatus,
    val appliedAtMillis: Long,
    /** Snapshot of the loader's rating at the time of application. Null if unavailable. */
    val ratingSnapshot: Float? = null
)

enum class OrderApplicationStatus {
    APPLIED,
    SELECTED,
    REJECTED,
    WITHDRAWN
}
