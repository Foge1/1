package com.loaderapp.features.orders.domain

enum class OrderApplicationStatus {
    APPLIED,
    SELECTED,
    REJECTED,
    WITHDRAWN
}

data class OrderApplication(
    val orderId: Long,
    val loaderId: String,
    val status: OrderApplicationStatus,
    val appliedAtMillis: Long,
    val ratingSnapshot: Float?
)

enum class OrderAssignmentStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELED
}

data class OrderAssignment(
    val orderId: Long,
    val loaderId: String,
    val status: OrderAssignmentStatus,
    val assignedAtMillis: Long,
    val startedAtMillis: Long?
)

data class OrderRulesContext(
    val activeAssignmentExists: Boolean = false,
    val activeAppliedCount: Int = 0
)
