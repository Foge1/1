package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStateMachineTest {

    private val stateMachine = OrderStateMachine(OrdersLimits(maxActiveApplications = 3))

    private fun baseOrder(
        status: OrderStatus = OrderStatus.STAFFING,
        workersTotal: Int = 2,
        createdByUserId: String = "dispatcher-1",
        applications: List<OrderApplication> = emptyList(),
        assignments: List<OrderAssignment> = emptyList()
    ): Order = Order(
        id = 1L,
        title = "Test Order",
        address = "Test Address",
        pricePerHour = 100.0,
        orderTime = OrderTime.Soon,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = workersTotal,
        tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"),
        status = status,
        createdByUserId = createdByUserId,
        applications = applications,
        assignments = assignments
    )

    private fun application(loaderId: String, status: OrderApplicationStatus) = OrderApplication(
        orderId = 1L,
        loaderId = loaderId,
        status = status,
        appliedAtMillis = 0L
    )

    private val loaderActor = CurrentUser(id = "loader-1", role = Role.LOADER)
    private val creatorDispatcher = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER)
    private val otherDispatcher = CurrentUser(id = "dispatcher-2", role = Role.DISPATCHER)

    @Test
    fun `apply block reasons are typed`() {
        val actionsInThisOrderAssignment = stateMachine.actionsFor(
            baseOrder(),
            loaderActor,
            OrderRulesContext(loaderHasActiveAssignmentInThisOrder = true)
        )
        assertEquals(OrderActionBlockReason.AlreadyAssignedToOrder, actionsInThisOrderAssignment.applyDisabledReason)

        val actionsActiveAssignment = stateMachine.actionsFor(
            baseOrder(),
            loaderActor,
            OrderRulesContext(activeAssignmentExists = true)
        )
        assertEquals(OrderActionBlockReason.ActiveAssignmentExists, actionsActiveAssignment.applyDisabledReason)

        val actionsLimit = stateMachine.actionsFor(
            baseOrder(),
            loaderActor,
            OrderRulesContext(activeApplicationsForLimitCount = 3)
        )
        assertEquals(OrderActionBlockReason.ApplyLimitReached, actionsLimit.applyDisabledReason)

        val actionsAlreadyApplied = stateMachine.actionsFor(
            baseOrder(applications = listOf(application("loader-1", OrderApplicationStatus.APPLIED))),
            loaderActor
        )
        assertEquals(OrderActionBlockReason.AlreadyApplied, actionsAlreadyApplied.applyDisabledReason)
    }

    @Test
    fun `apply limit comes from policy`() {
        val strictMachine = OrderStateMachine(OrdersLimits(maxActiveApplications = 1))
        val actions = strictMachine.actionsFor(
            baseOrder(),
            loaderActor,
            OrderRulesContext(activeApplicationsForLimitCount = 1)
        )
        assertFalse(actions.canApply)
        assertEquals(OrderActionBlockReason.ApplyLimitReached, actions.applyDisabledReason)
    }

    @Test
    fun `start requires selected equals workersTotal`() {
        val order = baseOrder(
            workersTotal = 2,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )
        val actions = stateMachine.actionsFor(order, creatorDispatcher)
        assertFalse(actions.canStart)
        assertEquals(OrderActionBlockReason.SelectedCountMismatch(1, 2), actions.startDisabledReason)
    }

    @Test
    fun `start and select are allowed only to dispatcher creator`() {
        val staffingOrder = baseOrder(
            workersTotal = 1,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )
        val actors = listOf(
            creatorDispatcher to true,
            otherDispatcher to false,
            loaderActor to false
        )

        actors.forEach { (actor, expectedAllowed) ->
            val selectResult = stateMachine.transition(staffingOrder, OrderEvent.SELECT, actor, now = 0L)
            assertEquals(expectedAllowed, selectResult is OrderTransitionResult.Success)

            val startResult = stateMachine.transition(staffingOrder, OrderEvent.START, actor, now = 0L)
            assertEquals(expectedAllowed, startResult is OrderTransitionResult.Success)
        }
    }

    @Test
    fun `apply transition and actions are consistent`() {
        val context = OrderRulesContext(activeAssignmentExists = true)
        val actions = stateMachine.actionsFor(baseOrder(), loaderActor, context)
        val transition = stateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.APPLY,
            actor = loaderActor,
            now = 0L,
            context = context
        )
        assertFalse(actions.canApply)
        assertTrue(transition is OrderTransitionResult.Failure)
        assertEquals(actions.applyDisabledReason, (transition as OrderTransitionResult.Failure).reason)
    }

    @Test
    fun `actions and transitions stay consistent for all exposed actions`() {
        val scenarios = listOf(
            Triple(baseOrder(), loaderActor, OrderRulesContext()),
            Triple(baseOrder(), loaderActor, OrderRulesContext(activeAssignmentExists = true)),
            Triple(baseOrder(status = OrderStatus.IN_PROGRESS), loaderActor, OrderRulesContext()),
            Triple(baseOrder(workersTotal = 1, applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))), creatorDispatcher, OrderRulesContext()),
            Triple(baseOrder(), otherDispatcher, OrderRulesContext())
        )

        scenarios.forEach { (order, actor, context) ->
            val actions = stateMachine.actionsFor(order, actor, context)
            val checks = listOf(
                OrderEvent.APPLY to Pair(actions.canApply, actions.applyDisabledReason),
                OrderEvent.WITHDRAW to Pair(actions.canWithdraw, actions.withdrawDisabledReason),
                OrderEvent.SELECT to Pair(actions.canSelect, null),
                OrderEvent.UNSELECT to Pair(actions.canUnselect, null),
                OrderEvent.START to Pair(actions.canStart, actions.startDisabledReason),
                OrderEvent.CANCEL to Pair(actions.canCancel, actions.cancelDisabledReason),
                OrderEvent.COMPLETE to Pair(actions.canComplete, actions.completeDisabledReason),
            )

            checks.forEach { (event, actionState) ->
                val transition = stateMachine.transition(order, event, actor, now = 0L, context = context)
                if (actionState.first) {
                    assertTrue("Expected success for $event in scenario $order/$actor", transition is OrderTransitionResult.Success)
                } else {
                    assertTrue("Expected failure for $event in scenario $order/$actor", transition is OrderTransitionResult.Failure)
                    val reason = (transition as OrderTransitionResult.Failure).reason
                    actionState.second?.let { assertEquals(it, reason) }
                }
            }
        }
    }

    @Test
    fun `apply is forbidden when loader has active assignment in this order`() {
        val context = OrderRulesContext(loaderHasActiveAssignmentInThisOrder = true)
        val actions = stateMachine.actionsFor(baseOrder(), loaderActor, context)
        assertFalse(actions.canApply)
        assertEquals(OrderActionBlockReason.AlreadyAssignedToOrder, actions.applyDisabledReason)
    }

    @Test
    fun `invalid in progress events return failure`() {
        val order = baseOrder(status = OrderStatus.IN_PROGRESS)
        val unsupported = listOf(
            OrderEvent.APPLY,
            OrderEvent.WITHDRAW,
            OrderEvent.SELECT,
            OrderEvent.UNSELECT,
            OrderEvent.START,
            OrderEvent.EXPIRE,
        )
        unsupported.forEach { event ->
            val result = stateMachine.transition(order, event, creatorDispatcher, 0L)
            assertTrue(result is OrderTransitionResult.Failure)
            assertEquals(
                OrderActionBlockReason.UnsupportedEventForStatus(event, OrderStatus.IN_PROGRESS),
                (result as OrderTransitionResult.Failure).reason
            )
        }
    }

    @Test
    fun `cancel and complete permissions are enforced`() {
        val cancelDenied = stateMachine.transition(baseOrder(), OrderEvent.CANCEL, otherDispatcher, 0L)
        assertEquals(
            OrderActionBlockReason.OnlyDispatcherCreatorCanCancel,
            (cancelDenied as OrderTransitionResult.Failure).reason
        )

        val completeDenied = stateMachine.transition(
            baseOrder(status = OrderStatus.IN_PROGRESS),
            OrderEvent.COMPLETE,
            loaderActor,
            0L,
            context = OrderRulesContext(loaderHasActiveAssignmentInThisOrder = false)
        )
        assertEquals(
            OrderActionBlockReason.OnlyDispatcherCreatorOrAssignedLoaderCanComplete,
            (completeDenied as OrderTransitionResult.Failure).reason
        )
    }

    @Test
    fun `terminal statuses reject every event`() {
        listOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED).forEach { status ->
            val result = stateMachine.transition(baseOrder(status = status), OrderEvent.START, creatorDispatcher, 0L)
            assertTrue(result is OrderTransitionResult.Failure)
            assertEquals(OrderActionBlockReason.TerminalStatus(status), (result as OrderTransitionResult.Failure).reason)
        }
    }

    @Test
    fun `happy paths remain unchanged`() {
        val startResult = stateMachine.transition(
            order = baseOrder(
                workersTotal = 1,
                applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
            ),
            event = OrderEvent.START,
            actor = creatorDispatcher,
            now = 0L
        )
        assertEquals(OrderStatus.IN_PROGRESS, (startResult as OrderTransitionResult.Success).order.status)

        val canApplyActions = stateMachine.actionsFor(baseOrder(), loaderActor)
        assertTrue(canApplyActions.canApply)
        assertNull(canApplyActions.applyDisabledReason)
    }
}
