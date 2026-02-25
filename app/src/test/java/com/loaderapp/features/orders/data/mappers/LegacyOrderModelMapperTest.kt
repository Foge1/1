package com.loaderapp.features.orders.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import org.junit.Test

class LegacyOrderModelMapperTest {

    @Test
    fun `toLegacyOrderModel maps staffing order to available legacy status`() {
        val order = Order(
            id = 42L,
            title = "Warehouse loading",
            address = "Some street",
            pricePerHour = 1000.0,
            orderTime = OrderTime.Soon,
            durationMin = 90,
            workersCurrent = 0,
            workersTotal = 3,
            tags = listOf("fragile"),
            meta = mapOf(
                Order.CREATED_AT_KEY to "1700000000000",
                "dispatcherId" to "19",
                "minWorkerRating" to "4.7"
            ),
            comment = "Call before arrival",
            status = OrderStatus.STAFFING,
            createdByUserId = "dispatcher-1"
        )

        val legacy = order.toLegacyOrderModel()

        assertEquals(42L, legacy.id)
        assertEquals(OrderStatusModel.AVAILABLE, legacy.status)
        assertEquals(19L, legacy.dispatcherId)
        assertEquals(4.7f, legacy.minWorkerRating)
        assertEquals(1, legacy.estimatedHours)
        assertTrue(legacy.isAsap)
    }
}
