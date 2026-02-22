package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersRepositoryImplRefreshTest {

    @Test
    fun `refresh does not expire soon orders`() = runBlocking {
        val repository = OrdersRepositoryImpl(FakeOrdersDao())
        repository.createOrder(
            Order(
                id = 0,
                title = "soon",
                address = "addr",
                pricePerHour = 100.0,
                orderTime = OrderTime.Soon,
                durationMin = 60,
                workersCurrent = 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = emptyMap(),
                status = OrderStatus.AVAILABLE,
                createdByUserId = "2",
                acceptedByUserId = null,
                acceptedAtMillis = null
            )
        )

        repository.refresh()

        val status = repository.observeOrders().first().first().status
        assertEquals(OrderStatus.AVAILABLE, status)
    }

    @Test
    fun `refresh expires exact orders older than threshold`() = runBlocking {
        val repository = OrdersRepositoryImpl(FakeOrdersDao())
        repository.createOrder(
            Order(
                id = 0,
                title = "old",
                address = "addr",
                pricePerHour = 100.0,
                orderTime = OrderTime.Exact(System.currentTimeMillis() - 120_000),
                durationMin = 60,
                workersCurrent = 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = emptyMap(),
                status = OrderStatus.AVAILABLE,
                createdByUserId = "2",
                acceptedByUserId = null,
                acceptedAtMillis = null
            )
        )

        repository.refresh()

        val status = repository.observeOrders().first().first().status
        assertEquals(OrderStatus.EXPIRED, status)
    }

    private class FakeOrdersDao : OrdersDao {
        private val orders = MutableStateFlow<List<OrderEntity>>(emptyList())

        override fun observeOrders(): Flow<List<OrderEntity>> = orders

        override suspend fun getOrders(): List<OrderEntity> = orders.value

        override suspend fun getOrderById(id: Long): OrderEntity? = orders.value.firstOrNull { it.id == id }

        override suspend fun insertOrder(order: OrderEntity): Long {
            val newId = (orders.value.maxOfOrNull { it.id } ?: 0L) + 1L
            orders.update { current -> current + order.copy(id = newId) }
            return newId
        }

        override suspend fun updateOrder(order: OrderEntity) {
            orders.update { current ->
                current.map { existing ->
                    if (existing.id == order.id) order else existing
                }
            }
        }
    }
}
