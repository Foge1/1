package com.loaderapp.features.orders.presentation

import androidx.lifecycle.ViewModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.OrdersLimits
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
import com.loaderapp.features.orders.testing.MainDispatcherRule
import com.loaderapp.features.orders.testing.TestAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResponsesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createdViewModels = mutableListOf<ResponsesViewModel>()

    private fun runResponsesTest(testBody: suspend TestScope.() -> Unit) =
        runTest(context = mainDispatcherRule.dispatcher) {
            try {
                testBody()
            } finally {
                val clearMethod = ViewModel::class.java.getDeclaredMethod("clear")
                clearMethod.isAccessible = true
                createdViewModels.forEach { viewModel -> clearMethod.invoke(viewModel) }
                createdViewModels.clear()
                advanceUntilIdle()
            }
        }

    @Test
    fun `refresh failure writes errorMessage to state`() =
        runResponsesTest {
            val repository = TestOrdersRepository()
            val viewModel = buildViewModel(repository)
            advanceUntilIdle()

            repository.refreshShouldFail = true
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("refresh failed", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `refresh retries loading and clears error on success`() =
        runResponsesTest {
            val repository = TestOrdersRepository()
            val viewModel = buildViewModel(repository)
            advanceUntilIdle()

            repository.refreshShouldFail = true
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals("refresh failed", viewModel.uiState.value.errorMessage)

            repository.refreshShouldFail = false
            viewModel.refresh()

            assertNull(viewModel.uiState.value.errorMessage)
            advanceUntilIdle()

            assertEquals(2, repository.refreshCalls)
            assertNull(viewModel.uiState.value.errorMessage)
            assertTrue(
                viewModel.uiState.value.items
                    .isNotEmpty(),
            )
        }

    private fun buildViewModel(repository: TestOrdersRepository): ResponsesViewModel {
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER))
        val stateMachine = OrderStateMachine(OrdersLimits())
        val observeOrderUiModels = ObserveOrderUiModelsUseCase(repository, currentUserProvider, stateMachine)
        val orchestrator =
            OrdersOrchestrator(
                createOrderUseCase = CreateOrderUseCase(repository, currentUserProvider),
                applyToOrderUseCase = ApplyToOrderUseCase(repository, currentUserProvider, stateMachine),
                withdrawApplicationUseCase = WithdrawApplicationUseCase(repository, currentUserProvider, stateMachine),
                selectApplicantUseCase = SelectApplicantUseCase(repository, currentUserProvider, stateMachine),
                unselectApplicantUseCase = UnselectApplicantUseCase(repository, currentUserProvider, stateMachine),
                startOrderUseCase = StartOrderUseCase(repository, currentUserProvider, stateMachine),
                cancelOrderUseCase = CancelOrderUseCase(repository, currentUserProvider, stateMachine),
                completeOrderUseCase = CompleteOrderUseCase(repository, currentUserProvider, stateMachine),
                refreshOrdersUseCase = RefreshOrdersUseCase(repository),
                appLogger = TestAppLogger(),
            )

        return ResponsesViewModel(
            observeOrderUiModels = observeOrderUiModels,
            getRespondersWithAvailability =
                com.loaderapp.features.orders.domain.usecase
                    .GetRespondersWithAvailabilityUseCase(repository),
            ordersOrchestrator = orchestrator,
        ).also { createdViewModels += it }
    }

    private class StaticCurrentUserProvider(
        private val currentUser: CurrentUser,
    ) : CurrentUserProvider {
        private val flow = MutableStateFlow(currentUser)

        override fun observeCurrentUser(): Flow<CurrentUser?> = flow

        override suspend fun getCurrentUserOrNull(): CurrentUser = currentUser

        override suspend fun requireCurrentUserOnce(): CurrentUser = currentUser
    }

    private class TestOrdersRepository : OrdersRepository {
        private val ordersFlow = MutableStateFlow(listOf(testOrder()))

        var refreshShouldFail: Boolean = false
        var refreshCalls: Int = 0

        override fun observeOrders(): Flow<List<Order>> = ordersFlow

        override suspend fun createOrder(order: Order): Long = 1L

        override suspend fun cancelOrder(
            id: Long,
            reason: String?,
        ) = Unit

        override suspend fun completeOrder(id: Long) = Unit

        override suspend fun refresh() {
            refreshCalls += 1
            if (refreshShouldFail) {
                error("refresh failed")
            }
            ordersFlow.value = ordersFlow.value
        }

        override suspend fun getOrderById(id: Long): Order? = ordersFlow.value.firstOrNull { it.id == id }

        override suspend fun applyToOrder(
            orderId: Long,
            loaderId: String,
            now: Long,
        ) = Unit

        override suspend fun withdrawApplication(
            orderId: Long,
            loaderId: String,
        ) = Unit

        override suspend fun selectApplicant(
            orderId: Long,
            loaderId: String,
        ) = Unit

        override suspend fun unselectApplicant(
            orderId: Long,
            loaderId: String,
        ) = Unit

        override suspend fun startOrder(
            orderId: Long,
            startedAtMillis: Long,
        ) = Unit

        override suspend fun hasActiveAssignment(loaderId: String): Boolean = false

        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> = emptyMap()

        override suspend fun hasActiveAssignmentInOrder(
            orderId: Long,
            loaderId: String,
        ): Boolean = false

        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = 0
    }

    companion object {
        private fun testOrder(): Order =
            Order(
                id = 11L,
                title = "Переезд",
                address = "ул. Ленина, 1",
                pricePerHour = 300.0,
                orderTime = OrderTime.Exact(1_700_000_000_000),
                durationMin = 120,
                workersCurrent = 0,
                workersTotal = 2,
                tags = emptyList(),
                meta = emptyMap(),
                status = OrderStatus.STAFFING,
                createdByUserId = "dispatcher-1",
                applications =
                    listOf(
                        OrderApplication(
                            orderId = 11L,
                            loaderId = "loader-1",
                            status = OrderApplicationStatus.APPLIED,
                            appliedAtMillis = 1L,
                        ),
                    ),
            )
    }
}
