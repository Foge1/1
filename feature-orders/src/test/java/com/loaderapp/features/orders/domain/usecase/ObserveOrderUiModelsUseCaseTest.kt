package com.loaderapp.features.orders.domain.usecase

import app.cash.turbine.test
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.OrdersLimits
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveOrderUiModelsUseCaseTest {

    @Test
    fun `loader applyDisabledReason is set when active assignment exists`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(order(id = 1L, status = OrderStatus.STAFFING)),
            hasActiveAssignment = true,
            activeApplicationsForLimitCount = 0
        )
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER))
        val useCase = ObserveOrderUiModelsUseCase(repo, currentUserProvider, OrderStateMachine(OrdersLimits()))

        val result = useCase().first() as ObserveOrderUiModelsResult.Selected
        val models = result.orders

        assertEquals(1, models.size)
        assertTrue(!models.first().canApply)
        assertNotNull(models.first().applyDisabledReason)
    }

    @Test
    fun `loader applyDisabledReason is set when active applied count reached`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(order(id = 1L, status = OrderStatus.STAFFING)),
            hasActiveAssignment = false,
            activeApplicationsForLimitCount = 3
        )
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER))
        val useCase = ObserveOrderUiModelsUseCase(repo, currentUserProvider, OrderStateMachine(OrdersLimits()))

        val result = useCase().first() as ObserveOrderUiModelsResult.Selected
        val models = result.orders

        assertEquals(1, models.size)
        assertTrue(!models.first().canApply)
        assertNotNull(models.first().applyDisabledReason)
    }

    @Test
    fun `ui models stream emits when orders change`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            orders = listOf(order(id = 1L, status = OrderStatus.STAFFING))
        )
        val currentUserProvider = StaticCurrentUserProvider(CurrentUser("loader-1", Role.LOADER))
        val useCase = ObserveOrderUiModelsUseCase(repo, currentUserProvider, OrderStateMachine(OrdersLimits()))

        useCase().test {
            val first = (awaitItem() as ObserveOrderUiModelsResult.Selected).orders
            assertEquals(1, first.size)

            repo.emitOrders(
                listOf(
                    order(id = 1L, status = OrderStatus.STAFFING),
                    order(id = 2L, status = OrderStatus.STAFFING)
                )
            )

            val second = (awaitItem() as ObserveOrderUiModelsResult.Selected).orders
            assertEquals(2, second.size)
            assertEquals(listOf(1L, 2L), second.map { it.order.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class StaticCurrentUserProvider(currentUser: CurrentUser) : CurrentUserProvider {
        private val state = MutableStateFlow<CurrentUser?>(currentUser)
        override fun observeCurrentUser(): Flow<CurrentUser?> = state
        override suspend fun getCurrentUserOrNull(): CurrentUser? = state.value
        override suspend fun requireCurrentUserOnce(): CurrentUser = state.value ?: error("Current user is not selected")
    }

    private class InMemoryOrdersRepository(
        orders: List<Order>,
        private val hasActiveAssignment: Boolean = false,
        private val activeApplicationsForLimitCount: Int = 0,
    ) : OrdersRepository {
        private val state = MutableStateFlow(orders)

        override fun observeOrders(): Flow<List<Order>> = state

        suspend fun emitOrders(orders: List<Order>) {
            state.emit(orders)
        }

        override suspend fun createOrder(order: Order) {
            state.update { it + order }
        }

        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) = Unit
        override suspend fun hasActiveAssignment(loaderId: String): Boolean = hasActiveAssignment
        override suspend fun getBusyAssignments(loaderIds: Collection<String>): Map<String, Long> = emptyMap()
        override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean = false
        override suspend fun countActiveApplicationsForLimit(loaderId: String): Int = activeApplicationsForLimitCount
    }

    private fun order(id: Long, status: OrderStatus) = Order(
        id = id,
        title = "Order $id",
        address = "Address",
        pricePerHour = 100.0,
        orderTime = OrderTime.Soon,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = 1,
        tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"),
        status = status,
        createdByUserId = "dispatcher-1",
        applications = emptyList<OrderApplication>()
    )
}
