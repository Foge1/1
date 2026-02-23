package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeOrdersRepositoryTest {
    @Test
    fun `startOrder moves selected to assignments and rejects applied`() = runBlocking {
        val repository = FakeOrdersRepository()
        val orderId = createOrder(repository)

        repository.applyToOrder(orderId, loaderId = "loader-1", now = 10L)
        repository.applyToOrder(orderId, loaderId = "loader-2", now = 11L)
        repository.selectApplicant(orderId, loaderId = "loader-1")

        repository.startOrder(orderId, startedAtMillis = 100L)

        val order = repository.getOrderById(orderId)!!
        assertEquals(OrderStatus.IN_PROGRESS, order.status)
        assertEquals(OrderApplicationStatus.SELECTED, order.applications.first { it.loaderId == "loader-1" }.status)
        assertEquals(OrderApplicationStatus.REJECTED, order.applications.first { it.loaderId == "loader-2" }.status)
        assertEquals(1, order.assignments.size)
        assertEquals(OrderAssignmentStatus.ACTIVE, order.assignments.single().status)
    }

    @Test
    fun `cancel and complete update assignments statuses`() = runBlocking {
        val repository = FakeOrdersRepository()
        val firstOrderId = createOrder(repository)
        repository.applyToOrder(firstOrderId, "loader-1", 1L)
        repository.selectApplicant(firstOrderId, "loader-1")
        repository.startOrder(firstOrderId, 2L)

        repository.cancelOrder(firstOrderId)
        assertEquals(
            OrderAssignmentStatus.CANCELED,
            repository.getOrderById(firstOrderId)!!.assignments.single().status
        )

        val secondOrderId = createOrder(repository)
        repository.applyToOrder(secondOrderId, "loader-2", 3L)
        repository.selectApplicant(secondOrderId, "loader-2")
        repository.startOrder(secondOrderId, 4L)

        repository.completeOrder(secondOrderId)
        assertEquals(
            OrderAssignmentStatus.COMPLETED,
            repository.getOrderById(secondOrderId)!!.assignments.single().status
        )
    }

    @Test
    fun `count helpers return active assignments and applied applications`() = runBlocking {
        val repository = FakeOrdersRepository()
        val orderId = createOrder(repository)
        repository.applyToOrder(orderId, "loader-1", 10L)
        repository.applyToOrder(orderId, "loader-2", 11L)
        repository.selectApplicant(orderId, "loader-1")
        repository.startOrder(orderId, 12L)

        assertEquals(1, repository.countActiveAppliedApplications("loader-2"))
        assertTrue(repository.hasActiveAssignment("loader-1"))
        assertEquals(0, repository.countActiveAppliedApplications("loader-1"))
    }

    private suspend fun createOrder(repository: FakeOrdersRepository): Long {
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
                status = OrderStatus.STAFFING,
                createdByUserId = "dispatcher"
            )
        )
        return repository.observeOrders().first().last().id
    }
}
