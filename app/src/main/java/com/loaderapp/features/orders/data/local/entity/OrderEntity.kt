package com.loaderapp.features.orders.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val address: String,
    val pricePerHour: Double,
    val orderTimeType: String,
    val orderTimeExactMillis: Long?,
    val durationMin: Int,
    val workersCurrent: Int,
    val workersTotal: Int,
    val tags: List<String>,
    val meta: Map<String, String>,
    val comment: String?,
    val status: String,
    val createdByUserId: String
)

