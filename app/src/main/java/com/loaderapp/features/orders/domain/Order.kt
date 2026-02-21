package com.loaderapp.features.orders.domain

data class Order(
    val id: Long,
    val title: String,
    val address: String,
    val pricePerHour: Double,
    val dateTime: Long,
    val durationMin: Int,
    val workersCurrent: Int,
    val workersTotal: Int,
    val tags: List<String>,
    val meta: Map<String, String>,
    val comment: String? = null,
    val status: OrderStatus
)
