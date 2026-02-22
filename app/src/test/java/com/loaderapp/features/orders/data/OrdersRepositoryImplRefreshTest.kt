package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersRepositoryImplRefreshTest {

    @Test
    fun `refresh does not expire soon orders`() = runBlocking {
        val repository = OrdersRepositoryImpl()
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
                status = OrderStatus.AVAILABLE
            )
        )

        repository.refresh()

        val status = repository.observeOrders().first().first().status
        assertEquals(OrderStatus.AVAILABLE, status)
    }

    @Test
    fun `refresh expires exact orders older than threshold`() = runBlocking {
        val repository = OrdersRepositoryImpl()
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
                status = OrderStatus.AVAILABLE
            )
        )

        repository.refresh()

        val status = repository.observeOrders().first().first().status
        assertEquals(OrderStatus.EXPIRED, status)
    }
}
