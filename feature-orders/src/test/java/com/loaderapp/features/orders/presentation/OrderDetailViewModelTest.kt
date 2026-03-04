package com.loaderapp.features.orders.presentation

import androidx.lifecycle.SavedStateHandle
import com.loaderapp.features.orders.domain.Order
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
import com.loaderapp.features.orders.domain.usecase.ObserveOrderDetailUseCase
import com.loaderapp.features.orders.domain.usecase.ObserveOrderUiModelsUseCase
import com.loaderapp.features.orders.domain.usecase.SelectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.StartOrderUseCase
import com.loaderapp.features.orders.domain.usecase.UnselectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.WithdrawApplicationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads order detail state from feature-orders usecase`() =
        mainDispatcherRule.runTest {
            val repository = RecordingOrdersRepository(orders = listOf(testOrder(77L)))
            val vm = buildViewModel(repository)

            advanceUntilIdle()

            assertFalse(vm.uiState.value.loading)
            assertNotNull(vm.uiState.value.order)
            val orderId =
                vm.uiState.value.order
                    ?.order
                    ?.id
            val orderTitle =
                vm.uiState.value.order
                    ?.order
                    ?.title

            assertEquals(77L, orderId)
            assertEquals("Order title", orderTitle)
        }

    @Test
    fun `onApply calls apply usecase and repository mutation`() =
        mainDispatcherRule.runTest {
            val repository = RecordingOrdersRepository(orders = listOf(testOrder(77L)))
            val vm = buildViewModel(repository)
            advanceUntilIdle()

            vm.onApply()
            advanceUntilIdle()

            assertEquals(1, repository.applyCalls)
            assertEquals(77L, repository.lastApplyOrderId)
            assertFalse(vm.uiState.value.isActionInProgress)
        }

    private fun buildViewModel(repository: RecordingOrdersRepository): OrderDetailViewModel {
        val stateMachine = OrderStateMachine(OrdersLimits(maxActiveApplications = 3))
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER))
        val observeOrderUiModelsUseCase = ObserveOrderUiModelsUseCase(repository, currentUserProvider, stateMachine)
        val observeOrderDetailUseCase = ObserveOrderDetailUseCase(observeOrderUiModelsUseCase)

        return OrderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(OrderDetailRoute.ORDER_ID_ARG to 77L)),
            observeOrderDetailUseCase = observeOrderDetailUseCase,
            applyToOrderUseCase = ApplyToOrderUseCase(repository, currentUserProvider, stateMachine),
            withdrawApplicationUseCase = WithdrawApplicationUseCase(repository, currentUserProvider, stateMachine),
            selectApplicantUseCase = SelectApplicantUseCase(repository, currentUserProvider, stateMachine),
            unselectApplicantUseCase = UnselectApplicantUseCase(repository, currentUserProvider, stateMachine),
            startOrderUseCase = StartOrderUseCase(repository, currentUserProvider, stateMachine),
            cancelOrderUseCase = CancelOrderUseCase(repository, currentUserProvider, stateMachine),
            completeOrderUseCase = CompleteOrderUseCase(repository, currentUserProvider, stateMachine),
        )
    }

    private class StaticCurrentUserProvider(
        private val user: CurrentUser?,
    ) : CurrentUserProvider {
        override fun observeCurrentUser(): Flow<CurrentUser?> = flowOf(user)

        override suspend fun getCurrentUserOrNull(): CurrentUser? = user

        override suspend fun requireCurrentUserOnce(): CurrentUser = requireNotNull(user)
    }

    private class RecordingOrdersRepository(
        orders: List<Order>,
    ) : OrdersRepository {
        private val ordersFlow = MutableStateFlow(orders)

        var applyCalls: Int = 0
        var lastApplyOrderId: Long? = null

        override fun observeOrders(): Flow<List<Order>> = ordersFlow

        override suspend fun createOrder(order: Order): Long = order.id

        override suspend fun cancelOrder(
            id: Long,
            reason: String?,
        ) = Unit

        override suspend fun completeOrder(id: Long) = Unit

        override suspend fun refresh() = Unit

        override suspend fun getOrderById(id: Long): Order? = ordersFlow.value.firstOrNull { it.id == id }

        override suspend fun applyToOrder(
            orderId: Long,
            loaderId: String,
            now: Long,
        ) {
            applyCalls += 1
            lastApplyOrderId = orderId
        }

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

    private fun testOrder(id: Long): Order =
        Order(
            id = id,
            title = "Order title",
            address = "Address",
            pricePerHour = 100.0,
            orderTime = OrderTime.Exact(System.currentTimeMillis()),
            durationMin = 60,
            workersCurrent = 0,
            workersTotal = 2,
            tags = emptyList(),
            meta = mapOf("dispatcherId" to "dispatcher-1"),
            comment = null,
            status = OrderStatus.STAFFING,
            createdByUserId = "dispatcher-1",
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }

    fun runTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher, testBody = block)
}
