package com.loaderapp.features.orders.presentation.model

import com.loaderapp.domain.model.OrderStatusModel

data class OrderCardUiModel(
    val id: Long,
    val address: String,
    val dateTime: Long,
    val cargoDescription: String,
    val pricePerHour: Double,
    val estimatedHours: Int,
    val requiredWorkers: Int,
    val currentWorkers: Int,
    val status: OrderStatusModel,
    val comment: String,
    val minWorkerRating: Float,
    val dispatcherId: Long,
    val isAsap: Boolean,
)
