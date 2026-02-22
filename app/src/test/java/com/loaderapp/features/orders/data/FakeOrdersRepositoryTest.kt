package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeOrdersRepositoryTest {
    @Test
    fun `acceptOrder sets acceptedBy and acceptedAt`() = runBlocking {
        val repository = FakeOrdersRepository()
        repository.createOrder(
            Order(
                id = 0,
                title = "title",
                address = "address",
                pricePerHour = 100.0,
                orderTime = OrderTime.Soon,
                durationMin = 60,
                workersCurrent = 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = emptyMap(),
                createdByUserId = "dispatcher"
            )
        )

        val id = repository.observeOrders().first().first().id
        repository.acceptOrder(id, acceptedByUserId = "loader-1", acceptedAtMillis = 12345L)

        val acceptedOrder = repository.getOrderById(id)!!
        assertEquals(OrderStatus.IN_PROGRESS, acceptedOrder.status)
        assertEquals("loader-1", acceptedOrder.acceptedByUserId)
        assertEquals(12345L, acceptedOrder.acceptedAtMillis)
    }
}
