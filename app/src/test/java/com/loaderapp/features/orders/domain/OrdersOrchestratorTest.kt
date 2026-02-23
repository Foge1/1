package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.data.FakeOrdersRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersOrchestratorTest {
    @Test
    fun `fake repository starts staffing order`() = runBlocking {
        val repository = FakeOrdersRepository()
        repository.createOrder(
            Order(
                id = 0,
                title = "t",
                address = "a",
                pricePerHour = 100.0,
                orderTime = OrderTime.Soon,
                durationMin = 60,
                workersCurrent = 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = emptyMap(),
                status = OrderStatus.STAFFING,
                createdByUserId = "d"
            )
        )
        val orderId = repository.getOrderById(1L)?.id ?: 1L

        repository.applyToOrder(orderId, "l1", 1L)
        repository.selectApplicant(orderId, "l1")
        repository.startOrder(orderId, 2L)

        assertEquals(OrderStatus.IN_PROGRESS, repository.getOrderById(orderId)?.status)
    }
}
