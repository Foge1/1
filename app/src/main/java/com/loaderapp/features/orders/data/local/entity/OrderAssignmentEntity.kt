package com.loaderapp.features.orders.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "order_assignments",
    primaryKeys = ["orderId", "loaderId"],
    indices = [
        Index(value = ["loaderId", "status"])
    ]
)
data class OrderAssignmentEntity(
    val orderId: Long,
    val loaderId: String,
    /** OrderAssignmentStatus.name */
    val status: String,
    val assignedAtMillis: Long,
    val startedAtMillis: Long?
)
