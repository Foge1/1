package com.loaderapp.ui.components

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderCardFormattingTest {

    @Test
    fun `formatOrderTime returns asap label for asap orders`() {
        val order = testOrder(isAsap = true, dateTime = 1700000000000)

        assertEquals("Ближайшее время", formatOrderTime(order))
    }

    @Test
    fun `formatOrderDate returns today label for today date`() {
        val now = System.currentTimeMillis()

        assertEquals("Сегодня", formatOrderDate(now))
    }

    private fun testOrder(isAsap: Boolean, dateTime: Long): OrderModel = OrderModel(
        id = 1,
        address = "test",
        dateTime = dateTime,
        cargoDescription = "cargo",
        pricePerHour = 500.0,
        estimatedHours = 2,
        requiredWorkers = 1,
        minWorkerRating = 0f,
        status = OrderStatusModel.AVAILABLE,
        createdAt = dateTime,
        completedAt = null,
        workerId = null,
        dispatcherId = 1,
        workerRating = null,
        comment = "",
        isAsap = isAsap
    )
}
