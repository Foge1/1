package com.loaderapp.features.orders.domain

sealed interface OrderTime {
    data class Exact(val dateTimeMillis: Long) : OrderTime
    data object Soon : OrderTime
}

data class Order(
    val id: Long,
    val title: String,
    val address: String,
    val pricePerHour: Double,
    val orderTime: OrderTime,
    val durationMin: Int,
    val workersCurrent: Int,
    val workersTotal: Int,
    val tags: List<String>,
    val meta: Map<String, String>,
    val comment: String? = null,
    val status: OrderStatus,
    val createdByUserId: String,
    // New staffing model
    val applications: List<OrderApplication> = emptyList(),
    val assignments: List<OrderAssignment> = emptyList(),
    // Deprecated: kept for backward-compat with old use-sites; derived from assignments on read.
    @Deprecated(
        message = "Use assignments to find loader. Will be removed after Step 3.",
        replaceWith = ReplaceWith("assignments.firstOrNull()?.loaderId")
    )
    val acceptedByUserId: String? = null,
    @Deprecated(
        message = "Use assignments to find startedAtMillis. Will be removed after Step 3.",
        replaceWith = ReplaceWith("assignments.firstOrNull()?.startedAtMillis")
    )
    val acceptedAtMillis: Long? = null
) {
    val dateTime: Long
        get() = when (val time = orderTime) {
            is OrderTime.Exact -> time.dateTimeMillis
            OrderTime.Soon -> meta[CREATED_AT_KEY]?.toLongOrNull() ?: System.currentTimeMillis()
        }

    companion object {
        const val CREATED_AT_KEY = "createdAt"
        const val TIME_TYPE_KEY = "timeType"
        const val TIME_TYPE_SOON = "soon"
    }
}

data class OrderDraft(
    val title: String,
    val address: String,
    val pricePerHour: Double,
    val orderTime: OrderTime,
    val durationMin: Int,
    val workersCurrent: Int,
    val workersTotal: Int,
    val tags: List<String>,
    val meta: Map<String, String>,
    val comment: String? = null
)

