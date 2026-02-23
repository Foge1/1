package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.data.FakeOrdersRepository
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersOwnershipUseCasesTest {
    @Test
    fun `accept use case delegates to apply`() = runBlocking {
        val repository = FakeOrdersRepository()
        repository.createOrder(
            Order(
                id = 0,
                title = "t",
                address = "a",
                pricePerHour = 1.0,
                orderTime = OrderTime.Soon,
                durationMin = 60,
                workersCurrent = 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = emptyMap(),
                status = OrderStatus.STAFFING,
                createdByUserId = "dispatcher"
            )
        )

        repository.applyToOrder(1L, "loader", 100L)
        val application = repository.getOrderById(1L)!!.applications.single()

        assertEquals(OrderApplicationStatus.APPLIED, application.status)
    }
}
