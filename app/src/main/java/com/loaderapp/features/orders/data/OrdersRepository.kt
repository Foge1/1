package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import kotlinx.coroutines.flow.Flow

interface OrdersRepository {
    fun observeOrders(): Flow<List<Order>>
    suspend fun createOrder(order: Order)
    suspend fun acceptOrder(id: Long)
    suspend fun cancelOrder(id: Long, reason: String? = null)
    suspend fun completeOrder(id: Long)
    suspend fun refresh()
}
