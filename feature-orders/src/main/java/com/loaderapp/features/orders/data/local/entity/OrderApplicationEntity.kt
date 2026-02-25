package com.loaderapp.features.orders.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "order_applications",
    primaryKeys = ["orderId", "loaderId"],
    indices = [
        Index(value = ["loaderId", "status"])
    ]
)
data class OrderApplicationEntity(
    val orderId: Long,
    val loaderId: String,
    /** OrderApplicationStatus.name */
    val status: String,
    val appliedAtMillis: Long,
    val ratingSnapshot: Float?
)
