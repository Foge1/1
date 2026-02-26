package com.loaderapp.features.orders.presentation

import com.loaderapp.core.logging.AppLogger
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrdersLimits

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
import com.loaderapp.features.orders.domain.usecase.ApplyToOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createdViewModels = mutableListOf<OrdersViewModel>()

    @After
    fun tearDown() {
        createdViewModels.forEach { it.clearForTest() }
        createdViewModels.clear()
    }

    private fun OrdersViewModel.clearForTest() {
        val clearMethod = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("clear")
        clearMethod.isAccessible = true
        clearMethod.invoke(this)
    }

    // ── OrderUiModel: canApply / canWithdraw correctness ─────────────────────

    @Test
    fun `loader canApply false when activeAssignmentExists`() = mainDispatcherRule.runTest {
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
    fun `loader canApply false when activeApplicationsForLimitCount at limit`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            activeApplicationsForLimitCount = 3
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertFalse(available.first().canApply)
    }

    @Test
    fun `loader canApply true when no active assignment and below limit`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            hasActiveAssignment = false,
            activeApplicationsForLimitCount = 2
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        val available = viewModel.uiState.value.availableOrders
        assertTrue(available.isNotEmpty())
        assertTrue(available.first().canApply)
        assertNull(available.first().applyDisabledReason)
    }

    @Test
    fun `dispatcher creator canStart true only when selectedCount equals workersTotal`() = mainDispatcherRule.runTest {
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
    fun `dispatcher creator canStart false when not enough selected`() = mainDispatcherRule.runTest {
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
    fun `non-creator dispatcher canSelect false`() = mainDispatcherRule.runTest {
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


    @Test
    fun `not selected user updates ui state instead of crash`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING))
        )
        val viewModel = buildViewModel(repository, NullableCurrentUserProvider(null))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.requiresUserSelection)
        assertTrue(state.availableOrders.isEmpty())
        assertFalse(state.loading)
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `refresh success toggles refreshing true to false`() = mainDispatcherRule.runTest {
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
    fun `refresh failure emits snackbar and clears refreshing`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Success)
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        repository.refreshMode = ExecutionMode.Failure
        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertNotNull(viewModel.uiState.value.errorMessage)
        snackbarCollector.cancel()
    }

    @Test
    fun `refresh cancellation clears refreshing without snackbar`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Success)
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        repository.refreshMode = ExecutionMode.Cancel
        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertEquals("cancelled", viewModel.uiState.value.errorMessage)
        assertFalse(snackbarCollector.isCompleted)
        snackbarCollector.cancel()
    }

    // ── Pending actions ───────────────────────────────────────────────────────

    @Test
    fun `pending action added then removed on success`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING)),
            applyMode = ExecutionMode.Success
        )
        val viewModel = buildViewModel(repository, loaderUser)
        advanceUntilIdle()

        viewModel.onApplyClicked(1L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.pendingActions.contains(1L))
    }

    @Test
    fun `pending action removed even after cancellation`() = mainDispatcherRule.runTest {
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
        assertEquals("cancelled", viewModel.uiState.value.errorMessage)
        assertFalse(snackbarCollector.isCompleted)
        snackbarCollector.cancel()
    }


    @Test
    fun `responses badge is zero for empty orders`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(orders = emptyList())
        val viewModel = buildViewModel(repository, dispatcherUser)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.responsesBadge.totalResponses == 0)
    }

    @Test
    fun `responses badge counts available orders responses only`() = mainDispatcherRule.runTest {
        val available = testOrder(
            id = 1L,
            status = OrderStatus.STAFFING,
            applications = listOf(OrderApplication(1L, "l1", OrderApplicationStatus.APPLIED, 0L))
        )
        val inProgress = testOrder(
            id = 2L,
            status = OrderStatus.IN_PROGRESS,
            applications = listOf(
                OrderApplication(2L, "l2", OrderApplicationStatus.APPLIED, 0L),
                OrderApplication(2L, "l3", OrderApplicationStatus.APPLIED, 0L)
            )
        )
        val history = testOrder(
            id = 3L,
            status = OrderStatus.COMPLETED,
            applications = listOf(OrderApplication(3L, "l4", OrderApplicationStatus.APPLIED, 0L))
        )
        val repository = TestOrdersRepository(orders = listOf(available, inProgress, history))
        val viewModel = buildViewModel(repository, dispatcherUser)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.responsesBadge.totalResponses == 1)
    }

    @Test
    fun `responses badge resets to zero when user is not selected`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(
            orders = listOf(
                testOrder(
                    id = 1L,
                    status = OrderStatus.STAFFING,
                    applications = listOf(OrderApplication(1L, "l1", OrderApplicationStatus.APPLIED, 0L))
                )
            )
        )
        val userProvider = NullableCurrentUserProvider(dispatcherUser)
        val viewModel = buildViewModel(repository, userProvider)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.responsesBadge.totalResponses == 1)

        userProvider.emit(null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.responsesBadge.totalResponses == 0)
    }


    @Test
    fun `Given selected dispatcher When current user becomes null Then uiState requires user selection and clears order lists`() = mainDispatcherRule.runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.STAFFING))
        )
        val userProvider = NullableCurrentUserProvider(dispatcherUser)
        val viewModel = buildViewModel(repository, userProvider)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.availableOrders.isNotEmpty())

        userProvider.emit(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.requiresUserSelection)
        assertTrue(state.availableOrders.isEmpty())
        assertTrue(state.inProgressOrders.isEmpty())
        assertTrue(state.historyOrders.isEmpty())
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private fun TestScope.buildViewModel(
        repository: TestOrdersRepository,
        user: CurrentUser
    ): OrdersViewModel = buildViewModel(repository, StaticCurrentUserProvider(user))

    private fun TestScope.buildViewModel(
        repository: TestOrdersRepository,
        userProvider: CurrentUserProvider
    ): OrdersViewModel {
        val stateMachine = OrderStateMachine(OrdersLimits())
        val applyUseCase = ApplyToOrderUseCase(repository, userProvider, stateMachine)
        val orchestrator = OrdersOrchestrator(
            createOrderUseCase = CreateOrderUseCase(repository, userProvider),
            applyToOrderUseCase = applyUseCase,
            withdrawApplicationUseCase = WithdrawApplicationUseCase(repository, userProvider, stateMachine),
            selectApplicantUseCase = SelectApplicantUseCase(repository, userProvider, stateMachine),
            unselectApplicantUseCase = UnselectApplicantUseCase(repository, userProvider, stateMachine),
            startOrderUseCase = StartOrderUseCase(repository, userProvider, stateMachine),
            cancelOrderUseCase = CancelOrderUseCase(repository, userProvider, stateMachine),
            completeOrderUseCase = CompleteOrderUseCase(repository, userProvider, stateMachine),
            refreshOrdersUseCase = RefreshOrdersUseCase(repository),
            appLogger = TestAppLogger
        )
        return OrdersViewModel(
            observeOrderUiModels = ObserveOrderUiModelsUseCase(
                repository = repository,
                currentUserProvider = userProvider,
                stateMachine = stateMachine,
            ),
            ordersOrchestrator = orchestrator
        ).also { createdViewModels += it }
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    private enum class ExecutionMode { Success, Failure, Exception, Cancel }

    // ── Test doubles ──────────────────────────────────────────────────────────

    private class StaticCurrentUserProvider(private val user: CurrentUser) : CurrentUserProvider {
        override fun observeCurrentUser(): Flow<CurrentUser?> = flowOf(user)
        override suspend fun getCurrentUserOrNull(): CurrentUser? = user
        override suspend fun requireCurrentUserOnce(): CurrentUser = user
    }

    private class NullableCurrentUserProvider(initial: CurrentUser?) : CurrentUserProvider {
        private val state = MutableStateFlow(initial)
        override fun observeCurrentUser(): Flow<CurrentUser?> = state
        override suspend fun getCurrentUserOrNull(): CurrentUser? = state.value
        override suspend fun requireCurrentUserOnce(): CurrentUser = state.value ?: error("Current user is not selected")
        suspend fun emit(user: CurrentUser?) { state.emit(user) }
    }

    private class TestOrdersRepository(
        orders: List<Order> = emptyList(),
        var refreshMode: ExecutionMode = ExecutionMode.Success,
        var applyMode: ExecutionMode = ExecutionMode.Success,
        private val hasActiveAssignment: Boolean = false,
        private val activeApplicationsForLimitCount: Int = 0
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
        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> = emptyMap()
        override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean = false
        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = activeApplicationsForLimitCount

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

    class MainDispatcherRule(
        val testDispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) { Dispatchers.setMain(testDispatcher) }
        override fun finished(description: Description) { Dispatchers.resetMain() }

        fun runTest(
            dispatchTimeoutMs: Long = 60_000L,
            testBody: suspend TestScope.() -> Unit
        ) = kotlinx.coroutines.test.runTest(
            testDispatcher.scheduler,
            dispatchTimeoutMs = dispatchTimeoutMs,
            testBody = testBody
        )
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val loaderUser = CurrentUser(id = "loader-1", role = Role.LOADER)
    private val dispatcherUser = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER)

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
        applications = applications,
        assignments = assignments
    )
}


private object TestAppLogger : AppLogger {
    override fun d(tag: String, message: String) = Unit
}
