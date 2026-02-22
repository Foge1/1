package com.loaderapp.features.orders.ui

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.usecase.AcceptOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
        val repository = TestOrdersRepository(refreshDelayMs = 100)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.refreshing)
    }

    @Test
    fun `refresh failure toggles refreshing true to false and emits snackbar`() = runTest {
        val repository = TestOrdersRepository(refreshDelayMs = 100)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()
        repository.failRefresh = true

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }

        viewModel.refresh()

        assertTrue(viewModel.uiState.value.refreshing)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
        assertTrue(viewModel.uiState.value.errorMessage != null)
        assertTrue(snackbarCollector.isCompleted)
    }

    @Test
    fun `pending action is added and removed on both success and failure`() = runTest {
        val repository = TestOrdersRepository(
            orders = listOf(
                testOrder(id = 1L, status = OrderStatus.AVAILABLE),
                testOrder(id = 2L, status = OrderStatus.COMPLETED)
            ),
            acceptDelayMs = 100
        )
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.onAcceptClicked(1L)
        assertTrue(viewModel.uiState.value.pendingActions.contains(1L))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.pendingActions.contains(1L))

        val snackbarCollector = backgroundScope.launch { viewModel.snackbarMessage.first() }
        viewModel.onAcceptClicked(2L)
        assertTrue(viewModel.uiState.value.pendingActions.contains(2L))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.pendingActions.contains(2L))
        assertTrue(snackbarCollector.isCompleted)
    }

    private fun TestScope.buildViewModel(repository: TestOrdersRepository): OrdersViewModel {
        val orchestrator = OrdersOrchestrator(
            createOrderUseCase = CreateOrderUseCase(repository),
            acceptOrderUseCase = AcceptOrderUseCase(repository),
            cancelOrderUseCase = CancelOrderUseCase(repository),
            completeOrderUseCase = CompleteOrderUseCase(repository),
            refreshOrdersUseCase = RefreshOrdersUseCase(repository)
        )
        return OrdersViewModel(
            observeOrdersUseCase = ObserveOrdersUseCase(repository),
            ordersOrchestrator = orchestrator
        )
    }

    private class TestOrdersRepository(
        orders: List<Order> = emptyList(),
        private val refreshDelayMs: Long = 0,
        private val acceptDelayMs: Long = 0
    ) : OrdersRepository {

        var failRefresh: Boolean = false

        private val state = MutableStateFlow(orders)

        override fun observeOrders(): Flow<List<Order>> = state

        override suspend fun createOrder(order: Order) {
            state.update { it + order }
        }

        override suspend fun acceptOrder(id: Long) {
            if (acceptDelayMs > 0) delay(acceptDelayMs)
            state.update { current ->
                current.map { order ->
                    if (order.id == id) order.copy(status = OrderStatus.IN_PROGRESS) else order
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
            if (refreshDelayMs > 0) delay(refreshDelayMs)
            if (failRefresh) error("refresh failed")
        }

        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }
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
            status = status
        )
    }
}
