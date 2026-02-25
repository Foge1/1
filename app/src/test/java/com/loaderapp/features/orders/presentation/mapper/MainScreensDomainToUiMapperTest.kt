package com.loaderapp.features.orders.presentation.mapper

import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignment
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.presentation.OrderUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreensDomainToUiMapperTest {

    @Test
    fun `Given domain order When mapping for order card Then uses active assignments and metadata defaults`() {
        val domain = testOrder(
            assignments = listOf(
                OrderAssignment(orderId = 10L, loaderId = "loader-1", status = OrderAssignmentStatus.ACTIVE, assignedAtMillis = 1L),
                OrderAssignment(orderId = 10L, loaderId = "loader-2", status = OrderAssignmentStatus.COMPLETED, assignedAtMillis = 2L)
            ),
            meta = mapOf("dispatcherId" to "bad-value")
        )

        val ui = domain.toUiModel()

        assertEquals(1, ui.currentWorkers)
        assertEquals(0L, ui.dispatcherId)
        assertEquals(0f, ui.minWorkerRating)
        assertEquals(OrderStatusModel.AVAILABLE, ui.status)
        assertTrue(ui.isAsap)
    }

    @Test
    fun `Given order ui model When mapping for order card Then selected workers are not lower than active assignments`() {
        val order = testOrder(
            assignments = listOf(
                OrderAssignment(orderId = 10L, loaderId = "loader-1", status = OrderAssignmentStatus.ACTIVE, assignedAtMillis = 3L)
            ),
            applications = listOf(
                OrderApplication(10L, "loader-1", OrderApplicationStatus.SELECTED, 1L),
                OrderApplication(10L, "loader-2", OrderApplicationStatus.SELECTED, 2L),
                OrderApplication(10L, "loader-3", OrderApplicationStatus.APPLIED, 3L)
            )
        )

        val orderUiModel = OrderUiModel(
            order = order,
            currentUserId = "dispatcher-1",
            currentUserRole = Role.DISPATCHER,
            canApply = false,
            applyBlockReason = null,
            canWithdraw = false,
            withdrawBlockReason = null,
            canSelect = true,
            canUnselect = true,
            canStart = true,
            startBlockReason = null,
            canCancel = true,
            cancelBlockReason = null,
            canComplete = false,
            completeBlockReason = null,
            canOpenChat = false
        )

        val ui = orderUiModel.toUiModel()

        assertEquals(2, ui.currentWorkers)
    }

    private fun testOrder(
        assignments: List<OrderAssignment> = emptyList(),
        applications: List<OrderApplication> = emptyList(),
        meta: Map<String, String> = mapOf("dispatcherId" to "42", "minWorkerRating" to "4.5")
    ): Order = Order(
        id = 10L,
        title = "Order",
        address = "Address",
        pricePerHour = 150.0,
        orderTime = OrderTime.Soon,
        durationMin = 90,
        workersCurrent = 0,
        workersTotal = 2,
        tags = listOf("fragile"),
        meta = meta,
        status = OrderStatus.STAFFING,
        createdByUserId = "dispatcher-1",
        applications = applications,
        assignments = assignments
    )
}
