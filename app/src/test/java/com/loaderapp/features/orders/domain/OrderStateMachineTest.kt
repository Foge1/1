package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain-only тесты OrderStateMachine (Step 3).
 * Покрывают все канонические правила модели "отозваться → отбор → старт диспетчером".
 */
class OrderStateMachineTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────────

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

    private fun assignment(loaderId: String, status: OrderAssignmentStatus = OrderAssignmentStatus.ACTIVE) =
        OrderAssignment(
            orderId = 1L,
            loaderId = loaderId,
            status = status,
            assignedAtMillis = 0L
        )

    private val loaderActor = CurrentUser(id = "loader-1", role = Role.LOADER)
    private val creatorDispatcher = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER)
    private val otherDispatcher = CurrentUser(id = "dispatcher-2", role = Role.DISPATCHER)

    // ─── actionsFor: Loader ───────────────────────────────────────────────────────

    @Test
    fun `loader canApply false when activeAssignmentExists`() {
        val order = baseOrder()
        val context = OrderRulesContext(activeAssignmentExists = true)

        val actions = OrderStateMachine.actionsFor(order, loaderActor, context)

        assertFalse(actions.canApply)
        assertNotNull(actions.applyDisabledReason)
    }

    @Test
    fun `loader canApply false when activeApplicationsForLimitCount at limit`() {
        val order = baseOrder()
        val context = OrderRulesContext(activeApplicationsForLimitCount = 3)

        val actions = OrderStateMachine.actionsFor(order, loaderActor, context)

        assertFalse(actions.canApply)
        assertNotNull(actions.applyDisabledReason)
    }

    @Test
    fun `loader canApply false when already applied to this order`() {
        val order = baseOrder(applications = listOf(application("loader-1", OrderApplicationStatus.APPLIED)))
        val context = OrderRulesContext(activeApplicationsForLimitCount = 1)

        val actions = OrderStateMachine.actionsFor(order, loaderActor, context)

        assertFalse(actions.canApply)
    }

    @Test
    fun `loader canApply false when order not in STAFFING`() {
        val order = baseOrder(status = OrderStatus.IN_PROGRESS)

        val actions = OrderStateMachine.actionsFor(order, loaderActor)

        assertFalse(actions.canApply)
    }

    @Test
    fun `loader canApply true when all conditions met`() {
        val order = baseOrder()
        val context = OrderRulesContext(activeAssignmentExists = false, activeApplicationsForLimitCount = 2)

        val actions = OrderStateMachine.actionsFor(order, loaderActor, context)

        assertTrue(actions.canApply)
        assertNull(actions.applyDisabledReason)
    }

    @Test
    fun `loader canWithdraw true when has APPLIED application`() {
        val order = baseOrder(applications = listOf(application("loader-1", OrderApplicationStatus.APPLIED)))

        val actions = OrderStateMachine.actionsFor(order, loaderActor)

        assertTrue(actions.canWithdraw)
    }

    @Test
    fun `loader canWithdraw false when no application on this order`() {
        val order = baseOrder()

        val actions = OrderStateMachine.actionsFor(order, loaderActor)

        assertFalse(actions.canWithdraw)
        assertNotNull(actions.withdrawDisabledReason)
    }

    @Test
    fun `loader canComplete true when has ACTIVE assignment in this order`() {
        val order = baseOrder(status = OrderStatus.IN_PROGRESS)
        val context = OrderRulesContext(loaderHasActiveAssignmentInThisOrder = true)

        val actions = OrderStateMachine.actionsFor(order, loaderActor, context)

        assertTrue(actions.canComplete)
        assertNull(actions.completeDisabledReason)
    }

    @Test
    fun `loader canComplete false when no ACTIVE assignment in this order`() {
        val order = baseOrder(status = OrderStatus.IN_PROGRESS)
        val context = OrderRulesContext(loaderHasActiveAssignmentInThisOrder = false)

        val actions = OrderStateMachine.actionsFor(order, loaderActor, context)

        assertFalse(actions.canComplete)
        assertNotNull(actions.completeDisabledReason)
    }

    // ─── actionsFor: Dispatcher ───────────────────────────────────────────────────

    @Test
    fun `dispatcher creator canStart true only when selectedCount equals workersTotal`() {
        val order = baseOrder(
            workersTotal = 2,
            applications = listOf(
                application("loader-1", OrderApplicationStatus.SELECTED),
                application("loader-2", OrderApplicationStatus.SELECTED)
            )
        )

        val actions = OrderStateMachine.actionsFor(order, creatorDispatcher)

        assertTrue(actions.canStart)
        assertNull(actions.startDisabledReason)
    }

    @Test
    fun `dispatcher creator canStart false when selectedCount less than workersTotal`() {
        val order = baseOrder(
            workersTotal = 2,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )

        val actions = OrderStateMachine.actionsFor(order, creatorDispatcher)

        assertFalse(actions.canStart)
        assertNotNull(actions.startDisabledReason)
    }

    @Test
    fun `non-creator dispatcher canStart false`() {
        val order = baseOrder(
            workersTotal = 1,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )

        val actions = OrderStateMachine.actionsFor(order, otherDispatcher)

        assertFalse(actions.canStart)
        assertNotNull(actions.startDisabledReason)
    }

    @Test
    fun `dispatcher creator canSelect and canUnselect in STAFFING`() {
        val order = baseOrder()

        val actions = OrderStateMachine.actionsFor(order, creatorDispatcher)

        assertTrue(actions.canSelect)
        assertTrue(actions.canUnselect)
    }

    @Test
    fun `non-creator dispatcher cannot select`() {
        val order = baseOrder()

        val actions = OrderStateMachine.actionsFor(order, otherDispatcher)

        assertFalse(actions.canSelect)
        assertFalse(actions.canUnselect)
    }

    @Test
    fun `terminal status returns all false actions for dispatcher`() {
        listOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED).forEach { status ->
            val order = baseOrder(status = status)
            val actions = OrderStateMachine.actionsFor(order, creatorDispatcher)

            assertFalse("canStart should be false for $status", actions.canStart)
            assertFalse("canCancel should be false for $status", actions.canCancel)
            assertFalse("canComplete should be false for $status", actions.canComplete)
        }
    }

    // ─── transition: APPLY ────────────────────────────────────────────────────────

    @Test
    fun `APPLY from STAFFING by loader returns Success`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.APPLY,
            actor = loaderActor,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.STAFFING, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `APPLY by dispatcher returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.APPLY,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `APPLY when activeAssignmentExists returns Failure`() {
        val context = OrderRulesContext(activeAssignmentExists = true)

        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.APPLY,
            actor = loaderActor,
            now = 0L,
            context = context
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `APPLY when activeApplicationsForLimitCount at limit returns Failure`() {
        val context = OrderRulesContext(activeApplicationsForLimitCount = 3)

        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.APPLY,
            actor = loaderActor,
            now = 0L,
            context = context
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    // ─── transition: START ────────────────────────────────────────────────────────

    @Test
    fun `START from STAFFING by creator dispatcher with full selection returns Success`() {
        val order = baseOrder(
            workersTotal = 1,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )

        val result = OrderStateMachine.transition(
            order = order,
            event = OrderEvent.START,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.IN_PROGRESS, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `START by non-creator dispatcher returns Failure`() {
        val order = baseOrder(
            workersTotal = 1,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )

        val result = OrderStateMachine.transition(
            order = order,
            event = OrderEvent.START,
            actor = otherDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `START by loader returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.START,
            actor = loaderActor,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `START when selectedCount less than workersTotal returns Failure`() {
        val order = baseOrder(
            workersTotal = 2,
            applications = listOf(application("loader-1", OrderApplicationStatus.SELECTED))
        )

        val result = OrderStateMachine.transition(
            order = order,
            event = OrderEvent.START,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    // ─── transition: CANCEL ───────────────────────────────────────────────────────

    @Test
    fun `CANCEL in STAFFING by creator dispatcher returns Success`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.CANCEL,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.CANCELED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `CANCEL in STAFFING by non-creator dispatcher returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.CANCEL,
            actor = otherDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `CANCEL in STAFFING by loader returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.CANCEL,
            actor = loaderActor,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `CANCEL in IN_PROGRESS by creator dispatcher returns Success`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS),
            event = OrderEvent.CANCEL,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.CANCELED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `CANCEL in IN_PROGRESS by non-creator returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS),
            event = OrderEvent.CANCEL,
            actor = otherDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    // ─── transition: COMPLETE ─────────────────────────────────────────────────────

    @Test
    fun `COMPLETE in IN_PROGRESS by loader without ACTIVE assignment returns Failure`() {
        val context = OrderRulesContext(loaderHasActiveAssignmentInThisOrder = false)

        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS),
            event = OrderEvent.COMPLETE,
            actor = loaderActor,
            now = 0L,
            context = context
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `COMPLETE in IN_PROGRESS by loader with ACTIVE assignment returns Success`() {
        val context = OrderRulesContext(loaderHasActiveAssignmentInThisOrder = true)

        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS),
            event = OrderEvent.COMPLETE,
            actor = loaderActor,
            now = 0L,
            context = context
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.COMPLETED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `COMPLETE in IN_PROGRESS by creator dispatcher returns Success`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS),
            event = OrderEvent.COMPLETE,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.COMPLETED, (result as OrderTransitionResult.Success).order.status)
    }

    @Test
    fun `COMPLETE in IN_PROGRESS by non-creator dispatcher returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.IN_PROGRESS),
            event = OrderEvent.COMPLETE,
            actor = otherDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `COMPLETE from STAFFING returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(status = OrderStatus.STAFFING),
            event = OrderEvent.COMPLETE,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    // ─── transition: terminal statuses ───────────────────────────────────────────

    @Test
    fun `terminal statuses reject any transition`() {
        val terminals = listOf(OrderStatus.COMPLETED, OrderStatus.CANCELED, OrderStatus.EXPIRED)
        val events = listOf(OrderEvent.APPLY, OrderEvent.CANCEL, OrderEvent.START, OrderEvent.EXPIRE)

        terminals.forEach { status ->
            events.forEach { event ->
                val result = OrderStateMachine.transition(
                    order = baseOrder(status = status),
                    event = event,
                    actor = creatorDispatcher,
                    now = 0L
                )
                assertTrue(
                    "Expected Failure for status=$status, event=$event",
                    result is OrderTransitionResult.Failure
                )
            }
        }
    }

    // ─── transition: EXPIRE ───────────────────────────────────────────────────────

    @Test
    fun `EXPIRE from STAFFING returns EXPIRED`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.EXPIRE,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
        assertEquals(OrderStatus.EXPIRED, (result as OrderTransitionResult.Success).order.status)
    }

    // ─── transition: SELECT / UNSELECT ────────────────────────────────────────────

    @Test
    fun `SELECT by creator dispatcher returns Success`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.SELECT,
            actor = creatorDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Success)
    }

    @Test
    fun `SELECT by non-creator dispatcher returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.SELECT,
            actor = otherDispatcher,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

    @Test
    fun `UNSELECT by loader returns Failure`() {
        val result = OrderStateMachine.transition(
            order = baseOrder(),
            event = OrderEvent.UNSELECT,
            actor = loaderActor,
            now = 0L
        )

        assertTrue(result is OrderTransitionResult.Failure)
    }

}
