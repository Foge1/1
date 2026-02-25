package com.loaderapp.features.orders.presentation

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.presentation.mapper.toOrderModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderUiModelMappingTest {

    @Test
    fun `toOrderModel keeps workerId null regardless of workersCurrent and applications`() {
        val order = Order(
            id = 1L,
            title = "Order",
            address = "Addr",
            pricePerHour = 120.0,
            orderTime = OrderTime.Soon,
            durationMin = 120,
            workersCurrent = 3,
            workersTotal = 5,
            tags = listOf("fragile"),
            meta = mapOf(Order.CREATED_AT_KEY to "1700000000000"),
            status = OrderStatus.STAFFING,
            createdByUserId = "dispatcher-1",
            applications = listOf(
                OrderApplication(
                    orderId = 1L,
                    loaderId = "loader-1",
                    status = OrderApplicationStatus.SELECTED,
                    appliedAtMillis = 1000L
                )
            )
        )

        val legacy = order.toOrderModel()

        assertEquals(3, order.workersCurrent)
        assertEquals(5, legacy.requiredWorkers)
        assertNull(legacy.workerId)
    }
}
