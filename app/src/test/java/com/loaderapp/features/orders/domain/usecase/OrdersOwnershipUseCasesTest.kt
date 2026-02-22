package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersOwnershipUseCasesTest {
    @Test
    fun `createOrder sets createdBy and resets acceptance`() = runBlocking {
        val repo = InMemoryOrdersRepository()
        val useCase = CreateOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("10", Role.DISPATCHER)))
        useCase(baseOrder())
        val created = repo.observeOrders().first().first()
        assertEquals("10", created.createdByUserId)
        assertNull(created.acceptedByUserId)
        assertEquals(OrderStatus.AVAILABLE, created.status)
    }

    @Test
    fun `acceptOrder sets acceptedBy and moves to in progress`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "20")))
        val useCase = AcceptOrderUseCase(repo, StaticCurrentUserProvider(CurrentUser("30", Role.LOADER)))
        val result = useCase(1)
        assertTrue(result is UseCaseResult.Success)
        val accepted = repo.getOrderById(1)!!
        assertEquals("30", accepted.acceptedByUserId)
        assertEquals(OrderStatus.IN_PROGRESS, accepted.status)
    }

    @Test
    fun `observe dispatcher returns only own created orders`() = runBlocking {
        val repo = InMemoryOrdersRepository(listOf(baseOrder(id = 1, createdBy = "1"), baseOrder(id = 2, createdBy = "2")))
        val useCase = ObserveOrdersForRoleUseCase(repo, StaticCurrentUserProvider(CurrentUser("1", Role.DISPATCHER)))
        assertEquals(listOf(1L), useCase().first().map { it.id })
    }

    @Test
    fun `observe loader returns available unaccepted and own in progress`() = runBlocking {
        val repo = InMemoryOrdersRepository(
            listOf(
                baseOrder(id = 1, status = OrderStatus.AVAILABLE, acceptedBy = null),
                baseOrder(id = 2, status = OrderStatus.AVAILABLE, acceptedBy = "x"),
                baseOrder(id = 3, status = OrderStatus.IN_PROGRESS, acceptedBy = "1"),
                baseOrder(id = 4, status = OrderStatus.IN_PROGRESS, acceptedBy = "2")
            )
        )
        val useCase = ObserveOrdersForRoleUseCase(repo, StaticCurrentUserProvider(CurrentUser("1", Role.LOADER)))
        assertEquals(listOf(1L, 3L), useCase().first().map { it.id })
    }

    private class StaticCurrentUserProvider(private val currentUser: CurrentUser) : CurrentUserProvider {
        override suspend fun getCurrentUser(): CurrentUser = currentUser
    }

    private class InMemoryOrdersRepository(initial: List<Order> = emptyList()) : OrdersRepository {
        private val state = MutableStateFlow(initial)
        override fun observeOrders(): Flow<List<Order>> = state
        override suspend fun createOrder(order: Order) { state.update { it + order.copy(id = 1) } }
        override suspend fun acceptOrder(id: Long, acceptedByUserId: String, acceptedAtMillis: Long) {
            state.update { list -> list.map { if (it.id == id) it.copy(status = OrderStatus.IN_PROGRESS, acceptedByUserId = acceptedByUserId, acceptedAtMillis = acceptedAtMillis) else it } }
        }
        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = state.value.firstOrNull { it.id == id }
    }

    private fun baseOrder(id: Long = 0, createdBy: String = "1", acceptedBy: String? = null, status: OrderStatus = OrderStatus.AVAILABLE) = Order(
        id = id,
        title = "Order",
        address = "Addr",
        pricePerHour = 100.0,
        orderTime = OrderTime.Soon,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = 1,
        tags = emptyList(),
        meta = emptyMap(),
        status = status,
        createdByUserId = createdBy,
        acceptedByUserId = acceptedBy,
        acceptedAtMillis = null
    )
}
