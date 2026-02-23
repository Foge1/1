package com.loaderapp.features.orders.domain

/**
 * Created for each SELECTED loader when an order is started.
 * Represents the actual work assignment.
 *
 * Lifecycle: ACTIVE → COMPLETED | CANCELED
 */
data class OrderAssignment(
    val orderId: Long,
    val loaderId: String,
    val status: OrderAssignmentStatus,
    val assignedAtMillis: Long,
    val startedAtMillis: Long? = null
)

enum class OrderAssignmentStatus {
    ACTIVE,
    COMPLETED,
    CANCELED
}
