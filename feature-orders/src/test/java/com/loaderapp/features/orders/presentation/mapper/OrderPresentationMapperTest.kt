package com.loaderapp.features.orders.presentation.mapper

import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPresentationMapperTest {

    @Test
    fun `toUiModel maps domain order to card ui model`() {
        val order = Order(
            id = 7L,
            title = "Переезд",
            address = "ул. Пушкина",
            pricePerHour = 1200.0,
            orderTime = OrderTime.Soon,
            durationMin = 130,
            workersCurrent = 0,
            workersTotal = 4,
            tags = listOf("мебель"),
            meta = mapOf("dispatcherId" to "55", "minWorkerRating" to "4.2"),
            comment = "Позвонить заранее",
            status = OrderStatus.STAFFING,
            createdByUserId = "disp",
            applications = listOf(
                OrderApplication(7L, "l-1", OrderApplicationStatus.SELECTED, 1L),
                OrderApplication(7L, "l-2", OrderApplicationStatus.APPLIED, 1L)
            )
        )

        val ui = order.toUiModel()

        assertEquals(7L, ui.id)
        assertEquals(OrderStatusModel.AVAILABLE, ui.status)
        assertEquals("мебель", ui.cargoDescription)
        assertEquals(2, ui.estimatedHours)
        assertEquals(55L, ui.dispatcherId)
        assertEquals(4.2f, ui.minWorkerRating)
        assertTrue(ui.isAsap)
    }
}
