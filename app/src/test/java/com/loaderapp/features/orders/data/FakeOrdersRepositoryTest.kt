package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("DEPRECATION")
class FakeOrdersRepositoryTest {

    private fun baseOrder(status: OrderStatus = OrderStatus.STAFFING) = Order(
        id = 0, title = "title", address = "address", pricePerHour = 100.0,
        orderTime = OrderTime.Soon, durationMin = 60, workersCurrent = 0, workersTotal = 1,
        tags = emptyList(), meta = emptyMap(), status = status, createdByUserId = "dispatcher"
    )

    @Test
    fun `startOrder creates ACTIVE assignments for SELECTED and rejects APPLIED`() = runBlocking {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        val orderId = repo.observeOrders().first().first().id

        repo.applyToOrder(orderId, "loader-1", 1000L)
        repo.applyToOrder(orderId, "loader-2", 1001L)
        repo.selectApplicant(orderId, "loader-1")

        repo.startOrder(orderId, startedAtMillis = 2000L)

        val order = repo.getOrderById(orderId)!!
        assertEquals(OrderStatus.IN_PROGRESS, order.status)

        val assignment = order.assignments.find { it.loaderId == "loader-1" }!!
        assertEquals(OrderAssignmentStatus.ACTIVE, assignment.status)
        assertEquals(2000L, assignment.startedAtMillis)

        val rejectedApp = order.applications.find { it.loaderId == "loader-2" }!!
        assertEquals(OrderApplicationStatus.REJECTED, rejectedApp.status)
    }

    @Test
    fun `cancelOrder transitions active assignments to CANCELED`() = runBlocking {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        val orderId = repo.observeOrders().first().first().id
        repo.applyToOrder(orderId, "loader-1", 1000L)
        repo.selectApplicant(orderId, "loader-1")
        repo.startOrder(orderId, 2000L)

        repo.cancelOrder(orderId)

        val order = repo.getOrderById(orderId)!!
        assertEquals(OrderStatus.CANCELED, order.status)
        val assignment = order.assignments.find { it.loaderId == "loader-1" }!!
        assertEquals(OrderAssignmentStatus.CANCELED, assignment.status)
    }

    @Test
    fun `completeOrder transitions active assignments to COMPLETED`() = runBlocking {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        val orderId = repo.observeOrders().first().first().id
        repo.applyToOrder(orderId, "loader-1", 1000L)
        repo.selectApplicant(orderId, "loader-1")
        repo.startOrder(orderId, 2000L)

        repo.completeOrder(orderId)

        val order = repo.getOrderById(orderId)!!
        assertEquals(OrderStatus.COMPLETED, order.status)
        val assignment = order.assignments.find { it.loaderId == "loader-1" }!!
        assertEquals(OrderAssignmentStatus.COMPLETED, assignment.status)
    }

    @Test
    fun `hasActiveAssignment returns true after startOrder`() = runBlocking {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        val orderId = repo.observeOrders().first().first().id
        repo.applyToOrder(orderId, "loader-1", 1000L)
        repo.selectApplicant(orderId, "loader-1")
        assertFalse(repo.hasActiveAssignment("loader-1"))
        repo.startOrder(orderId, 2000L)
        assertTrue(repo.hasActiveAssignment("loader-1"))
    }

    @Test
    fun `countActiveAppliedApplications counts correctly`() = runBlocking {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        repo.createOrder(baseOrder())
        val orders = repo.observeOrders().first()
        val id1 = orders[0].id
        val id2 = orders[1].id

        repo.applyToOrder(id1, "loader-1", 1000L)
        repo.applyToOrder(id2, "loader-1", 1001L)

        assertEquals(2, repo.countActiveAppliedApplications("loader-1"))

        repo.withdrawApplication(id1, "loader-1")
        assertEquals(1, repo.countActiveAppliedApplications("loader-1"))
    }
}
