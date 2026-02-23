package com.loaderapp.features.orders.domain.repository

import com.loaderapp.features.orders.domain.Order
import kotlinx.coroutines.flow.Flow

interface OrdersRepository {
    fun observeOrders(): Flow<List<Order>>
    suspend fun createOrder(order: Order)
    suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long)
    suspend fun withdrawApplication(orderId: Long, loaderId: String)
    suspend fun selectApplicant(orderId: Long, loaderId: String)
    suspend fun unselectApplicant(orderId: Long, loaderId: String)
    suspend fun startOrder(orderId: Long, startedAtMillis: Long)
    suspend fun cancelOrder(id: Long, reason: String? = null)
    suspend fun completeOrder(id: Long)
    suspend fun hasActiveAssignment(loaderId: String): Boolean
    suspend fun countActiveAppliedApplications(loaderId: String): Int
    suspend fun refresh()
    suspend fun getOrderById(id: Long): Order?
}
