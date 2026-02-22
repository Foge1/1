package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStateMachineTest {

    @Test
    fun `accept moves available order to in progress for loader`() {
        val order = baseOrder(status = OrderStatus.AVAILABLE)
        val actor = CurrentUser(id = "loader-1", role = Role.LOADER)

        val result = OrderStateMachine.transition(order, OrderEvent.ACCEPT, actor, now = 123L)

        assertTrue(result is OrderTransitionResult.Success)
        val transitioned = (result as OrderTransitionResult.Success).order
        assertEquals(OrderStatus.IN_PROGRESS, transitioned.status)
        assertEquals("loader-1", transitioned.acceptedByUserId)
        assertEquals(123L, transitioned.acceptedAtMillis)
    }

    @Test
    fun `expire moves available order to expired`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.AVAILABLE),
            event = OrderEvent.EXPIRE,
            actor = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER),
            now = 100L
        )

        assertEquals(OrderStatus.EXPIRED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `complete moves in progress order to completed for assigned loader`() {
        val order = baseOrder(status = OrderStatus.IN_PROGRESS, acceptedByUserId = "loader-1")
        val result = OrderStateMachine.transition(
            order,
            OrderEvent.COMPLETE,
            CurrentUser(id = "loader-1", role = Role.LOADER),
            now = 100L
        )

        assertEquals(OrderStatus.COMPLETED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `terminal statuses reject any transition`() {
        val statuses = listOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED)

        statuses.forEach { status ->
            val result = OrderStateMachine.transition(
                order = baseOrder(status = status),
                event = OrderEvent.CANCEL,
                actor = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER),
                now = 100L
            )
            assertTrue(result is OrderTransitionResult.Failure)
        }
    }

    @Test
    fun `invalid role cannot accept available order`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.AVAILABLE),
            event = OrderEvent.ACCEPT,
            actor = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER),
            now = 100L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `invalid actor cannot cancel available order`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.AVAILABLE, createdByUserId = "dispatcher-1"),
            event = OrderEvent.CANCEL,
            actor = CurrentUser(id = "dispatcher-2", role = Role.DISPATCHER),
            now = 100L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    private fun baseOrder(
        status: OrderStatus,
        createdByUserId: String = "dispatcher-1",
        acceptedByUserId: String? = null
    ): Order = Order(
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
        status = status,
        createdByUserId = createdByUserId,
        acceptedByUserId = acceptedByUserId,
        acceptedAtMillis = null
    )
}
