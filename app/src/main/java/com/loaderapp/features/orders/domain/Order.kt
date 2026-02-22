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
    val status: OrderStatus = OrderStatus.AVAILABLE,
    val createdByUserId: String = "",
    val acceptedByUserId: String? = null,
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
