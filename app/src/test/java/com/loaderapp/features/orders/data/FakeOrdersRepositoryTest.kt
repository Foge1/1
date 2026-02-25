package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeOrdersRepositoryTest {

    private fun baseOrder(status: OrderStatus = OrderStatus.STAFFING, workersTotal: Int = 1) = Order(
        id = 0, title = "title", address = "address", pricePerHour = 100.0,
        orderTime = OrderTime.Soon, durationMin = 60, workersCurrent = 0, workersTotal = workersTotal,
        tags = emptyList(), meta = emptyMap(), status = status, createdByUserId = "dispatcher"
    )

    @Test
    fun `createOrder always normalizes status to STAFFING`() = runTest {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder(status = OrderStatus.IN_PROGRESS))

        val order = repo.observeOrders().first().first()
        assertEquals(OrderStatus.STAFFING, order.status)
    }

    @Test
    fun `applyToOrder is idempotent and keeps first appliedAtMillis`() = runTest {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder(status = OrderStatus.STAFFING))
        val orderId = repo.observeOrders().first().first().id

        repo.applyToOrder(orderId, "loader-1", 1000L)
        repo.applyToOrder(orderId, "loader-1", 2000L)

        val order = repo.getOrderById(orderId)!!
        assertEquals(1, order.applications.size)
        assertEquals(1000L, order.applications.single().appliedAtMillis)
    }

    @Test
    fun `applyToOrder is ignored when order is not STAFFING`() = runTest {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder(status = OrderStatus.IN_PROGRESS))
        val orderId = repo.observeOrders().first().first().id

        // force non-staffing as runtime state transition would do
        repo.cancelOrder(orderId)

        repo.applyToOrder(orderId, "loader-1", 1000L)

        val order = repo.getOrderById(orderId)!!
        assertTrue(order.applications.isEmpty())
    }

    @Test
    fun `startOrder creates assignments only for SELECTED with correct timestamps and rejects APPLIED`() = runTest {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        val orderId = repo.observeOrders().first().first().id

        repo.applyToOrder(orderId, "loader-1", 1000L)
        repo.applyToOrder(orderId, "loader-2", 1001L)
        repo.applyToOrder(orderId, "loader-3", 1002L)
        repo.selectApplicant(orderId, "loader-1")

        repo.startOrder(orderId, startedAtMillis = 2000L)

        val order = repo.getOrderById(orderId)!!
        assertEquals(OrderStatus.IN_PROGRESS, order.status)

        assertEquals(1, order.assignments.size)
        val selectedAssignment = order.assignments.single()
        assertEquals("loader-1", selectedAssignment.loaderId)
        assertEquals(OrderAssignmentStatus.ACTIVE, selectedAssignment.status)
        assertEquals(1000L, selectedAssignment.assignedAtMillis)
        assertEquals(2000L, selectedAssignment.startedAtMillis)

        val selectedApp = order.applications.find { it.loaderId == "loader-1" }!!
        assertEquals(OrderApplicationStatus.SELECTED, selectedApp.status)
        val rejectedApp2 = order.applications.find { it.loaderId == "loader-2" }!!
        val rejectedApp3 = order.applications.find { it.loaderId == "loader-3" }!!
        assertEquals(OrderApplicationStatus.REJECTED, rejectedApp2.status)
        assertEquals(OrderApplicationStatus.REJECTED, rejectedApp3.status)
    }

    @Test
    fun `cancelOrder transitions active assignments to CANCELED`() = runTest {
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
    fun `completeOrder transitions active assignments to COMPLETED`() = runTest {
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
    fun `hasActiveAssignment returns true after startOrder`() = runTest {
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
    fun `countActiveApplicationsForLimit counts correctly`() = runTest {
        val repo = FakeOrdersRepository()
        repo.createOrder(baseOrder())
        repo.createOrder(baseOrder())
        val orders = repo.observeOrders().first()
        val id1 = orders[0].id
        val id2 = orders[1].id

        repo.applyToOrder(id1, "loader-1", 1000L)
        repo.applyToOrder(id2, "loader-1", 1001L)
        repo.selectApplicant(id2, "loader-1")

        assertEquals(2, repo.countActiveApplicationsForLimit("loader-1"))

        repo.withdrawApplication(id1, "loader-1")
        assertEquals(1, repo.countActiveApplicationsForLimit("loader-1"))
    }
}
