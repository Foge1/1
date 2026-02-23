package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStateMachineStep1Test {

    @Test
    fun `apply allowed for loader on staffing with free limits`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(status = OrderStatus.STAFFING),
            actor = actorLoader("loader-1"),
            context = OrderRulesContext(activeAssignmentExists = false, activeAppliedCount = 0)
        )

        assertTrue(actions.canApply)
        assertEquals(null, actions.applyDisabledReason)
    }

    @Test
    fun `apply blocked when loader already has active assignment`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(status = OrderStatus.STAFFING),
            actor = actorLoader("loader-1"),
            context = OrderRulesContext(activeAssignmentExists = true, activeAppliedCount = 0)
        )

        assertFalse(actions.canApply)
        assertEquals("У вас уже есть активный заказ", actions.applyDisabledReason)
    }

    @Test
    fun `apply blocked by active applies limit`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(status = OrderStatus.STAFFING),
            actor = actorLoader("loader-1"),
            context = OrderRulesContext(activeAssignmentExists = false, activeAppliedCount = 3)
        )

        assertFalse(actions.canApply)
        assertEquals("Лимит активных откликов: 3", actions.applyDisabledReason)
    }

    @Test
    fun `apply blocked when already applied`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                applications = listOf(application("loader-1", OrderApplicationStatus.APPLIED))
            ),
            actor = actorLoader("loader-1"),
            context = OrderRulesContext()
        )

        assertFalse(actions.canApply)
        assertEquals("Вы уже откликнулись", actions.applyDisabledReason)
    }

    @Test
    fun `apply blocked when already selected`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
            ),
            actor = actorLoader("loader-1"),
            context = OrderRulesContext()
        )

        assertFalse(actions.canApply)
        assertEquals("Вы уже отобраны", actions.applyDisabledReason)
    }

    @Test
    fun `dispatcher cannot apply`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(status = OrderStatus.STAFFING),
            actor = actorDispatcher("dispatcher-1"),
            context = OrderRulesContext()
        )

        assertFalse(actions.canApply)
        assertEquals("Только грузчик может откликнуться", actions.applyDisabledReason)
    }

    @Test
    fun `creator dispatcher can select on staffing`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(status = OrderStatus.STAFFING, createdBy = "dispatcher-1"),
            actor = actorDispatcher("dispatcher-1"),
            context = OrderRulesContext()
        )

        assertTrue(actions.canSelect)
    }

    @Test
    fun `non creator dispatcher cannot select`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(status = OrderStatus.STAFFING, createdBy = "dispatcher-1"),
            actor = actorDispatcher("dispatcher-2"),
            context = OrderRulesContext()
        )

        assertFalse(actions.canSelect)
    }

    @Test
    fun `start available only when selected count equals workers total`() {
        val canStart = OrderStateMachine.actionsFor(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                workersTotal = 2,
                createdBy = "dispatcher-1",
                applications = listOf(
                    application("loader-1", OrderApplicationStatus.SELECTED),
                    application("loader-2", OrderApplicationStatus.SELECTED)
                )
            ),
            actor = actorDispatcher("dispatcher-1"),
            context = OrderRulesContext()
        )

        val cannotStart = OrderStateMachine.actionsFor(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                workersTotal = 2,
                createdBy = "dispatcher-1",
                applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
            ),
            actor = actorDispatcher("dispatcher-1"),
            context = OrderRulesContext()
        )

        assertTrue(canStart.canStart)
        assertFalse(cannotStart.canStart)
    }

    @Test
    fun `transition start moves staffing to in progress for creator dispatcher`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                createdBy = "dispatcher-1",
                applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
            ),
            event = OrderEvent.START,
            actor = actorDispatcher("dispatcher-1"),
            now = 10L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.IN_PROGRESS, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `transition start fails for non creator dispatcher`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                createdBy = "dispatcher-1",
                applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
            ),
            event = OrderEvent.START,
            actor = actorDispatcher("dispatcher-2"),
            now = 10L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `cannot select when selected count already equals workers total`() {
        val actions = OrderStateMachine.actionsFor(
            order = baseOrder(
                status = OrderStatus.STAFFING,
                workersTotal = 1,
                createdBy = "dispatcher-1",
                applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
            ),
            actor = actorDispatcher("dispatcher-1"),
            context = OrderRulesContext()
        )

        assertFalse(actions.canSelect)
    }

    @Test
    fun `cancel on staffing allowed only for creator dispatcher`() {
        val order = baseOrder(status = OrderStatus.STAFFING, createdBy = "dispatcher-1")

        val creatorResult = OrderStateMachine.transition(order, OrderEvent.CANCEL, actorDispatcher("dispatcher-1"), 0L)
        val otherResult = OrderStateMachine.transition(order, OrderEvent.CANCEL, actorDispatcher("dispatcher-2"), 0L)

        assertTrue(creatorResult is OrderTransitionResult.Success)
        assertTrue(otherResult is OrderTransitionResult.Failure)
    }

    @Test
    fun `cancel on in progress allowed only for creator dispatcher`() {
        val order = baseOrder(status = OrderStatus.IN_PROGRESS, createdBy = "dispatcher-1")

        val creatorResult = OrderStateMachine.transition(order, OrderEvent.CANCEL, actorDispatcher("dispatcher-1"), 0L)
        val otherResult = OrderStateMachine.transition(order, OrderEvent.CANCEL, actorLoader("loader-1"), 0L)

        assertTrue(creatorResult is OrderTransitionResult.Success)
        assertTrue(otherResult is OrderTransitionResult.Failure)
    }

    @Test
    fun `in progress complete allowed for creator dispatcher`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS, createdBy = "dispatcher-1"),
            event = OrderEvent.COMPLETE,
            actor = actorDispatcher("dispatcher-1"),
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.COMPLETED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `in progress complete allowed for loader with active assignment`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(
                status = OrderStatus.IN_PROGRESS,
                assignments = listOf(assignment("loader-1", OrderAssignmentStatus.ACTIVE))
            ),
            event = OrderEvent.COMPLETE,
            actor = actorLoader("loader-1"),
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.COMPLETED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `in progress complete denied for loader without assignment`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(
                status = OrderStatus.IN_PROGRESS,
                assignments = listOf(assignment("loader-2", OrderAssignmentStatus.ACTIVE))
            ),
            event = OrderEvent.COMPLETE,
            actor = actorLoader("loader-1"),
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `terminal statuses reject all transitions`() {
        val terminalStatuses = listOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED)
        val events = OrderEvent.entries

        terminalStatuses.forEach { status ->
            events.forEach { event ->
                val result = OrderStateMachine.transition(
                    order = baseOrder(status = status),
                    event = event,
                    actor = actorDispatcher("dispatcher-1"),
                    now = 0L
                )

                assertTrue("Expected failure for $status + $event", result is OrderTransitionResult.Failure)
            }
        }
    }

    private fun baseOrder(
        status: OrderStatus,
        workersTotal: Int = 1,
        createdBy: String = "dispatcher-1",
        applications: List<OrderApplication> = emptyList(),
        assignments: List<OrderAssignment> = emptyList()
    ): Order {
        return Order(
            id = 1L,
            title = "Order",
            address = "Address",
            pricePerHour = 100.0,
            orderTime = OrderTime.Soon,
            durationMin = 60,
            workersCurrent = 0,
            workersTotal = workersTotal,
            tags = emptyList(),
            meta = mapOf(Order.CREATED_AT_KEY to "0"),
            status = status,
            createdByUserId = createdBy,
            applications = applications,
            assignments = assignments
        )
    }

    private fun actorLoader(id: String): CurrentUser = CurrentUser(id = id, role = Role.LOADER)

    private fun actorDispatcher(id: String): CurrentUser = CurrentUser(id = id, role = Role.DISPATCHER)

    private fun application(loaderId: String, status: OrderApplicationStatus): OrderApplication {
        return OrderApplication(
            orderId = 1L,
            loaderId = loaderId,
            status = status,
            appliedAtMillis = 1L,
            ratingSnapshot = 4.5f
        )
    }

    private fun assignment(loaderId: String, status: OrderAssignmentStatus): OrderAssignment {
        return OrderAssignment(
            orderId = 1L,
            loaderId = loaderId,
            status = status,
            assignedAtMillis = 1L,
            startedAtMillis = if (status == OrderAssignmentStatus.ACTIVE) 2L else null
        )
    }
}
