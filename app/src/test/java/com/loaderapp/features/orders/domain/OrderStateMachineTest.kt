package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStateMachineTest {

    @Test
    fun `staffing can expire`() {
        val result = OrderStateMachine.transition(
            order = Order(
                id = 1L,
                title = "Order",
                address = "Address",
                pricePerHour = 100.0,
                orderTime = OrderTime.Soon,
                durationMin = 60,
                workersCurrent = 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = mapOf(Order.CREATED_AT_KEY to "0"),
                status = OrderStatus.STAFFING,
                createdByUserId = "dispatcher-1"
            ),
            event = OrderEvent.EXPIRE,
            actor = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER),
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
    }
}
