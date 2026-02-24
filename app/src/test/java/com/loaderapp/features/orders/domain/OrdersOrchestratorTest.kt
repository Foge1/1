package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrdersLimits

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.usecase.ApplyToOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.SelectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.StartOrderUseCase
import com.loaderapp.features.orders.domain.usecase.UnselectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import com.loaderapp.features.orders.domain.usecase.WithdrawApplicationUseCase
import com.loaderapp.features.orders.ui.OrdersCommand
import com.loaderapp.features.orders.ui.OrdersOrchestrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersOrchestratorTest {

    // ── Core new-flow commands ────────────────────────────────────────────────

    @Test
    fun `Apply succeeds when order is STAFFING and no active assignment`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING))
        )
        val orchestrator = buildOrchestrator(repo, loaderUser)

        val result = orchestrator.execute(OrdersCommand.Apply(orderId = 1L))

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `Apply fails when loader already has an active assignment`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            hasActiveAssignment = true
        )
        val orchestrator = buildOrchestrator(repo, loaderUser)

        val result = orchestrator.execute(OrdersCommand.Apply(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Apply fails when loader has reached applied count limit`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            activeApplicationsForLimitCount = 3
        )
        val orchestrator = buildOrchestrator(repo, loaderUser)

        val result = orchestrator.execute(OrdersCommand.Apply(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Apply fails when order is not STAFFING`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.IN_PROGRESS))
        )
        val orchestrator = buildOrchestrator(repo, loaderUser)

        val result = orchestrator.execute(OrdersCommand.Apply(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Apply fails when actor is dispatcher`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING))
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Apply(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Select succeeds when dispatcher is creator`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = DISPATCHER_ID))
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Select(orderId = 1L, loaderId = "loader-x"))

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `Select fails when dispatcher is not creator`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = "other-dispatcher"))
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Select(orderId = 1L, loaderId = "loader-x"))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Unselect fails when dispatcher is not creator`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = "other-dispatcher"))
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Unselect(orderId = 1L, loaderId = "loader-x"))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Start succeeds when dispatcher is creator and selectedCount equals workersTotal`() = runBlocking {
        val selected = listOf(
            OrderApplication(orderId = 1L, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L),
            OrderApplication(orderId = 1L, loaderId = "l2", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repo = InMemoryOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = DISPATCHER_ID, workersTotal = 2, applications = selected)
            )
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Start(orderId = 1L))

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `Start fails when selectedCount less than workersTotal`() = runBlocking {
        val selected = listOf(
            OrderApplication(orderId = 1L, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repo = InMemoryOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = DISPATCHER_ID, workersTotal = 2, applications = selected)
            )
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Start(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Start fails when dispatcher is not creator`() = runBlocking {
        val selected = listOf(
            OrderApplication(orderId = 1L, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repo = InMemoryOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = "other-dispatcher", workersTotal = 1, applications = selected)
            )
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Start(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Start fails when actor is loader`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING))
        )
        val orchestrator = buildOrchestrator(repo, loaderUser)

        val result = orchestrator.execute(OrdersCommand.Start(orderId = 1L))

        assertTrue(result is UseCaseResult.Failure)
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    fun `Cancel fails for non-creator in STAFFING`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = "other"))
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Cancel(orderId = 1L, reason = null))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `Cancel succeeds for creator in STAFFING`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = DISPATCHER_ID))
        )
        val orchestrator = buildOrchestrator(repo, dispatcherUser)

        val result = orchestrator.execute(OrdersCommand.Cancel(orderId = 1L, reason = null))

        assertTrue(result is UseCaseResult.Success)
    }

    // ── Refresh / Create ──────────────────────────────────────────────────────

    @Test
    fun `Refresh returns Success`() = runBlocking {
        val result = buildOrchestrator(InMemoryOrdersRepository(), loaderUser)
            .execute(OrdersCommand.Refresh)
        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `Create returns Success for valid draft`() = runBlocking {
        val result = buildOrchestrator(InMemoryOrdersRepository(), dispatcherUser)
            .execute(OrdersCommand.Create(testOrderDraft()))
        assertTrue(result is UseCaseResult.Success)
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private fun buildOrchestrator(repo: OrdersRepository, user: CurrentUser): OrdersOrchestrator {
        val userProvider = StaticCurrentUserProvider(user)
        val stateMachine = OrderStateMachine(OrdersLimits())
        val applyUseCase = ApplyToOrderUseCase(repo, userProvider, stateMachine)
        return OrdersOrchestrator(
            createOrderUseCase = CreateOrderUseCase(repo, userProvider),
            applyToOrderUseCase = applyUseCase,
            withdrawApplicationUseCase = WithdrawApplicationUseCase(repo, userProvider, stateMachine),
            selectApplicantUseCase = SelectApplicantUseCase(repo, userProvider, stateMachine),
            unselectApplicantUseCase = UnselectApplicantUseCase(repo, userProvider, stateMachine),
            startOrderUseCase = StartOrderUseCase(repo, userProvider, stateMachine),
            cancelOrderUseCase = CancelOrderUseCase(repo, userProvider, stateMachine),
            completeOrderUseCase = CompleteOrderUseCase(repo, userProvider, stateMachine),
            refreshOrdersUseCase = RefreshOrdersUseCase(repo)
        )
    }

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class StaticCurrentUserProvider(private val user: CurrentUser) : CurrentUserProvider {
        override fun observeCurrentUser(): Flow<CurrentUser?> = flowOf(user)
        override suspend fun getCurrentUserOrNull(): CurrentUser? = user
        override suspend fun requireCurrentUserOnce(): CurrentUser = user
    }

    private class InMemoryOrdersRepository(
        private val orders: MutableList<Order> = mutableListOf(),
        private val hasActiveAssignment: Boolean = false,
        private val activeApplicationsForLimitCount: Int = 0
    ) : OrdersRepository {
        constructor(orders: List<Order>, hasActiveAssignment: Boolean = false, activeApplicationsForLimitCount: Int = 0)
            : this(orders.toMutableList(), hasActiveAssignment, activeApplicationsForLimitCount)

        override fun observeOrders(): Flow<List<Order>> = emptyFlow()

        override suspend fun createOrder(order: Order) {
            orders.add(order.copy(id = if (order.id == 0L) 1L else order.id, status = OrderStatus.STAFFING))
        }

        override suspend fun cancelOrder(id: Long, reason: String?) = mutate(id) { it.copy(status = OrderStatus.CANCELED) }
        override suspend fun completeOrder(id: Long) = mutate(id) { it.copy(status = OrderStatus.COMPLETED) }
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = orders.firstOrNull { it.id == id }
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) = Unit
        override suspend fun hasActiveAssignment(loaderId: String): Boolean = hasActiveAssignment
        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> = emptyMap()
        override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean = false
        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = activeApplicationsForLimitCount

        private fun mutate(id: Long, transform: (Order) -> Order) {
            val i = orders.indexOfFirst { it.id == id }
            if (i >= 0) orders[i] = transform(orders[i])
        }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val loaderUser = CurrentUser(id = LOADER_ID, role = Role.LOADER)
    private val dispatcherUser = CurrentUser(id = DISPATCHER_ID, role = Role.DISPATCHER)

    private fun testOrderDraft() = OrderDraft(
        title = "Test", address = "Address", pricePerHour = 100.0, orderTime = OrderTime.Soon,
        durationMin = 60, workersCurrent = 0, workersTotal = 2, tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"), comment = null
    )

    private fun testOrder(
        id: Long,
        status: OrderStatus,
        createdBy: String = DISPATCHER_ID,
        workersTotal: Int = 2,
        applications: List<OrderApplication> = emptyList()
    ) = Order(
        id = id, title = "Test", address = "Address", pricePerHour = 100.0,
        orderTime = OrderTime.Soon, durationMin = 60, workersCurrent = 0,
        workersTotal = workersTotal, tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"), status = status,
        createdByUserId = createdBy, applications = applications
    )

    private companion object {
        const val LOADER_ID = "loader-1"
        const val DISPATCHER_ID = "dispatcher-1"
    }
}
