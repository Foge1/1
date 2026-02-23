package com.loaderapp.features.orders.ui

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignment
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderRulesContext
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.usecase.AcceptOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ApplyToOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrdersForRoleUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.SelectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.StartOrderUseCase
import com.loaderapp.features.orders.domain.usecase.UnselectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.WithdrawApplicationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class OrdersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── OrderUiModel: canApply / canWithdraw correctness ─────────────────────

    @Test
    fun `loader canApply false when activeAssignmentExists`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            hasActiveAssignment = true
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertFalse(available.first().canApply)
        assertNotNull(available.first().applyDisabledReason)
    }

    @Test
    fun `loader canApply false when activeAppliedCount at limit`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            activeAppliedCount = 3
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertFalse(available.first().canApply)
    }

    @Test
    fun `loader canApply true when no active assignment and below limit`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            hasActiveAssignment = false,
            activeAppliedCount = 2
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertTrue(available.first().canApply)
        assertNull(available.first().applyDisabledReason)
    }

    @Test
    fun `dispatcher creator canStart true only when selectedCount equals workersTotal`() = runTest {
        val applications = listOf(
            OrderApplication(orderId = 1L, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L),
            OrderApplication(orderId = 1L, loaderId = "l2", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repository = TestOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.STAFFING, workersTotal = 2, applications = applications)
            )
        )
        val viewModel = buildViewModel(repository, dispatcherUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertTrue(available.first().canStart)
        assertNull(available.first().startDisabledReason)
    }

    @Test
    fun `dispatcher creator canStart false when not enough selected`() = runTest {
        val applications = listOf(
            OrderApplication(orderId = 1L, loaderId = "l1", status = OrderApplicationStatus.SELECTED, appliedAtMillis = 0L)
        )
        val repository = TestOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.STAFFING, workersTotal = 2, applications = applications)
            )
        )
        val viewModel = buildViewModel(repository, dispatcherUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertFalse(available.first().canStart)
        assertNotNull(available.first().startDisabledReason)
    }

    @Test
    fun `non-creator dispatcher canSelect false`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.STAFFING, createdBy = "other-dispatcher")
            )
        )
        val viewModel = buildViewModel(repository, dispatcherUser) // dispatcherUser = "dispatcher-1"
        advanceUntilIdle()

        // non-creator dispatcher sees no orders (filtered by ObserveOrdersForRoleUseCase)
        // so available should be empty for non-creator
        // (this is correct behavior — dispatcher sees only own orders)
        assertTrue(viewModel.uiState.value.availableOrders.isEmpty())
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `refresh success toggles refreshing true to false`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Success)
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.refreshing)
    }

    @Test
    fun `refresh failure emits snackbar and clears refreshing`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Failure)
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(snackbarCollector.isCompleted)
    }

    @Test
    fun `refresh cancellation clears refreshing without snackbar`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Cancel)
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(snackbarCollector.isCompleted)
        snackbarCollector.cancel()
    }

    // ── Pending actions ───────────────────────────────────────────────────────

    @Test
    fun `pending action added then removed on success`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            applyMode = ExecutionMode.Success
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        viewModel.onApplyClicked(1L)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingActions.contains(1L))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.pendingActions.contains(1L))
    }

    @Test
    fun `pending action removed even after cancellation`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            applyMode = ExecutionMode.Cancel
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }
        viewModel.onApplyClicked(1L)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingActions.contains(1L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.pendingActions.contains(1L))
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(snackbarCollector.isCompleted)
        snackbarCollector.cancel()
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private fun TestScope.buildViewModel(
        repository: TestOrdersRepository,
        user: CurrentUser
    ): OrdersViewModel {
        val userProvider = StaticCurrentUserProvider(user)
        val observeForRole = ObserveOrdersForRoleUseCase(repository, userProvider)
        val applyUseCase = ApplyToOrderUseCase(repository, userProvider)
        @Suppress("DEPRECATION")
        val orchestrator = OrdersOrchestrator(
            createOrderUseCase = CreateOrderUseCase(repository, userProvider),
            applyToOrderUseCase = applyUseCase,
            withdrawApplicationUseCase = WithdrawApplicationUseCase(repository, userProvider),
            selectApplicantUseCase = SelectApplicantUseCase(repository, userProvider),
            unselectApplicantUseCase = UnselectApplicantUseCase(repository, userProvider),
            startOrderUseCase = StartOrderUseCase(repository, userProvider),
            cancelOrderUseCase = CancelOrderUseCase(repository, userProvider),
            completeOrderUseCase = CompleteOrderUseCase(repository, userProvider),
            refreshOrdersUseCase = RefreshOrdersUseCase(repository),
            acceptOrderUseCase = AcceptOrderUseCase(applyUseCase)
        )
        return OrdersViewModel(
            observeOrderUiModels = ObserveOrderUiModelsUseCase(
                repository = repository,
                currentUserProvider = userProvider,
                observeOrdersForRoleUseCase = observeForRole
            ),
            ordersOrchestrator = orchestrator
        )
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    private enum class ExecutionMode { Success, Failure, Exception, Cancel }

    // ── Test doubles ──────────────────────────────────────────────────────────

    private class StaticCurrentUserProvider(private val user: CurrentUser) : CurrentUserProvider {
        override fun observeCurrentUser(): Flow<CurrentUser> = flowOf(user)
        override suspend fun getCurrentUser(): CurrentUser = user
    }

    private class TestOrdersRepository(
        orders: List<Order> = emptyList(),
        var refreshMode: ExecutionMode = ExecutionMode.Success,
        var applyMode: ExecutionMode = ExecutionMode.Success,
        private val hasActiveAssignment: Boolean = false,
        private val activeAppliedCount: Int = 0
    ) : OrdersRepository {

        private val state = MutableStateFlow(orders)

        override fun observeOrders(): Flow<List<Order>> = state

        override suspend fun createOrder(order: Order) {
            state.update { it + order }
        }

        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) {
            executeMode(applyMode, "apply failed")
        }

        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) = Unit

        override suspend fun hasActiveAssignment(loaderId: String): Boolean = hasActiveAssignment
        override suspend fun countActiveAppliedApplications(loaderId: String): Int = activeAppliedCount

        override suspend fun cancelOrder(id: Long, reason: String?) {
            state.update { orders ->
                orders.map { if (it.id == id) it.copy(status = OrderStatus.CANCELED) else it }
            }
        }

        override suspend fun completeOrder(id: Long) {
            state.update { orders ->
                orders.map { if (it.id == id) it.copy(status = OrderStatus.COMPLETED) else it }
            }
        }

        override suspend fun refresh() {
            executeMode(refreshMode, "refresh failed")
        }

        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }

        private suspend fun executeMode(mode: ExecutionMode, message: String) {
            delay(100)
            when (mode) {
                ExecutionMode.Success -> Unit
                ExecutionMode.Failure -> error(message)
                ExecutionMode.Exception -> throw IllegalStateException(message)
                ExecutionMode.Cancel -> throw CancellationException("cancelled")
            }
        }
    }

    // ── Rules ─────────────────────────────────────────────────────────────────

    private class MainDispatcherRule(
        private val dispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
        override fun finished(description: Description) { Dispatchers.resetMain() }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val loaderUser = CurrentUser(id = "loader-1", role = Role.LOADER)
    private val dispatcherUser = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER)

    @Suppress("DEPRECATION")
    private fun testOrder(
        id: Long,
        status: OrderStatus,
        createdBy: String = "dispatcher-1",
        workersTotal: Int = 2,
        applications: List<OrderApplication> = emptyList(),
        assignments: List<OrderAssignment> = emptyList()
    ): Order = Order(
        id = id,
        title = "order-$id",
        address = "addr",
        pricePerHour = 100.0,
        orderTime = OrderTime.Soon,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = workersTotal,
        tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"),
        status = status,
        createdByUserId = createdBy,
        acceptedByUserId = null,
        acceptedAtMillis = null,
        applications = applications,
        assignments = assignments
    )
}
