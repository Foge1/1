package com.loaderapp.features.orders.ui

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.usecase.AcceptOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrdersForRoleUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class OrdersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh success toggles refreshing true to false`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Success)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.refreshing)
    }

    @Test
    fun `refresh failure toggles refreshing true to false and emits snackbar`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Failure)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertTrue(viewModel.uiState.value.errorMessage != null)
        assertTrue(snackbarCollector.isCompleted)
    }


    @Test
    fun `refresh exception toggles refreshing true to false and emits snackbar`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Exception)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertTrue(viewModel.uiState.value.errorMessage != null)
        assertTrue(snackbarCollector.isCompleted)
    }

    @Test
    fun `refresh cancellation toggles refreshing true->false and emits no snackbar`() = runTest {
        val repository = TestOrdersRepository(refreshMode = ExecutionMode.Cancel)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertTrue(viewModel.uiState.value.errorMessage == null)
        assertFalse(snackbarCollector.isCompleted)
        snackbarCollector.cancel()
    }

    @Test
    fun `pending action is added and removed on both success and failure`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.AVAILABLE),
                testOrder(id = 2L, status = OrderStatus.COMPLETED)
            ),
            acceptMode = ExecutionMode.Success
        )
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.onAcceptClicked(1L)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingActions.contains(1L))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.pendingActions.contains(1L))

        repository.acceptMode = ExecutionMode.Failure
        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }
        viewModel.onAcceptClicked(2L)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingActions.contains(2L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.pendingActions.contains(2L))
        assertTrue(snackbarCollector.isCompleted)
    }

    @Test
    fun `pending action is removed even when cancelled`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(testOrder(id = 1L, status = OrderStatus.AVAILABLE)),
            acceptMode = ExecutionMode.Cancel
        )
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.onAcceptClicked(1L)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingActions.contains(1L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.pendingActions.contains(1L))
        assertTrue(viewModel.uiState.value.errorMessage == null)
        assertFalse(snackbarCollector.isCompleted)
        snackbarCollector.cancel()
    }

    private fun TestScope.buildViewModel(repository: TestOrdersRepository): OrdersViewModel {
        val orchestrator = OrdersOrchestrator(
            createOrderUseCase = CreateOrderUseCase(repository, TestCurrentUserProvider()),
            acceptOrderUseCase = AcceptOrderUseCase(repository, TestCurrentUserProvider()),
            cancelOrderUseCase = CancelOrderUseCase(repository, TestCurrentUserProvider()),
            completeOrderUseCase = CompleteOrderUseCase(repository, TestCurrentUserProvider()),
            refreshOrdersUseCase = RefreshOrdersUseCase(repository)
        )
        return OrdersViewModel(
            observeOrdersUseCase = ObserveOrdersForRoleUseCase(repository, TestCurrentUserProvider()),
            ordersOrchestrator = orchestrator
        )
    }

    private enum class ExecutionMode {
        Success,
        Failure,
        Exception,
        Cancel
    }

    private class TestOrdersRepository(
        orders: List<Order> = emptyList(),
        var refreshMode: ExecutionMode = ExecutionMode.Success,
        var acceptMode: ExecutionMode = ExecutionMode.Success
    ) : OrdersRepository {

        private val state = MutableStateFlow(orders)

        override fun observeOrders(): Flow<List<Order>> = state

        override suspend fun createOrder(order: Order) {
            state.update { it + order }
        }

        override suspend fun acceptOrder(id: Long, acceptedByUserId: String, acceptedAtMillis: Long) {
            executeMode(acceptMode, "accept failed")
            state.update { current ->
                current.map { order ->
                    if (order.id == id) order.copy(status = OrderStatus.IN_PROGRESS, acceptedByUserId = acceptedByUserId, acceptedAtMillis = acceptedAtMillis) else order
                }
            }
        }

        override suspend fun cancelOrder(id: Long, reason: String?) {
            state.update { current ->
                current.map { order ->
                    if (order.id == id) order.copy(status = OrderStatus.CANCELED) else order
                }
            }
        }

        override suspend fun completeOrder(id: Long) {
            state.update { current ->
                current.map { order ->
                    if (order.id == id) order.copy(status = OrderStatus.COMPLETED) else order
                }
            }
        }

        override suspend fun refresh() {
            executeMode(refreshMode, "refresh failed")
        }

        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }

        private suspend fun executeMode(mode: ExecutionMode, failureMessage: String) {
            delay(100)
            when (mode) {
                ExecutionMode.Success -> Unit
                ExecutionMode.Failure -> error(failureMessage)
                ExecutionMode.Exception -> throw IllegalStateException(failureMessage)
                ExecutionMode.Cancel -> throw CancellationException("cancelled")
            }
        }
    }

    private class MainDispatcherRule(
        private val dispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }


    private class TestCurrentUserProvider : CurrentUserProvider {
        private val currentUser = CurrentUser(id = "1", role = Role.LOADER)
        override fun observeCurrentUser(): Flow<CurrentUser> = flowOf(currentUser)
        override suspend fun getCurrentUser(): CurrentUser = currentUser
    }

    private fun testOrder(id: Long, status: OrderStatus): Order {
        return Order(
            id = id,
            title = "order-$id",
            address = "addr",
            pricePerHour = 100.0,
            orderTime = OrderTime.Soon,
            durationMin = 60,
            workersCurrent = 0,
            workersTotal = 2,
            tags = emptyList(),
            meta = mapOf(Order.CREATED_AT_KEY to "0"),
            status = status,
            createdByUserId = "2",
            acceptedByUserId = null,
            acceptedAtMillis = null
        )
    }
}
