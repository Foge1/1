package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.OrderDraft
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.usecase.AcceptOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import com.loaderapp.features.orders.ui.OrdersCommand
import com.loaderapp.features.orders.ui.OrdersOrchestrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersOrchestratorTest {

    @Test
    fun `execute accept returns failure for invalid status`() = runBlocking {
        val repository = InMemoryOrdersRepository(
            orders = listOf(
                testOrder(id = 7L, status = OrderStatus.COMPLETED)
            )
        )
        val orchestrator = buildOrchestrator(repository)

        val result = orchestrator.execute(OrdersCommand.Accept(orderId = 7L))

        assertTrue(result is UseCaseResult.Failure)
    }

    @Test
    fun `execute refresh returns success`() = runBlocking {
        val orchestrator = buildOrchestrator(InMemoryOrdersRepository())

        val result = orchestrator.execute(OrdersCommand.Refresh)

        assertTrue(result is UseCaseResult.Success)
    }

    @Test
    fun `execute create returns success for valid order`() = runBlocking {
        val orchestrator = buildOrchestrator(InMemoryOrdersRepository())

        val result = orchestrator.execute(OrdersCommand.Create(testOrderDraft()))

        assertTrue(result is UseCaseResult.Success)
    }

    private fun buildOrchestrator(repository: OrdersRepository): OrdersOrchestrator {
        return OrdersOrchestrator(
            createOrderUseCase = CreateOrderUseCase(repository, TestCurrentUserProvider()),
            acceptOrderUseCase = AcceptOrderUseCase(repository, TestCurrentUserProvider()),
            cancelOrderUseCase = CancelOrderUseCase(repository, TestCurrentUserProvider()),
            completeOrderUseCase = CompleteOrderUseCase(repository, TestCurrentUserProvider()),
            refreshOrdersUseCase = RefreshOrdersUseCase(repository)
        )
    }

    private class InMemoryOrdersRepository(
        private val orders: MutableList<Order> = mutableListOf()
    ) : OrdersRepository {

        constructor(orders: List<Order>) : this(orders.toMutableList())

        override fun observeOrders(): Flow<List<Order>> = emptyFlow()

        override suspend fun createOrder(order: Order) {
            orders.add(order.copy(id = if (order.id == 0L) 1L else order.id))
        }

        override suspend fun acceptOrder(id: Long, acceptedByUserId: String, acceptedAtMillis: Long) {
            mutate(id) { it.copy(status = OrderStatus.IN_PROGRESS, acceptedByUserId = acceptedByUserId, acceptedAtMillis = acceptedAtMillis) }
        }

        override suspend fun cancelOrder(id: Long, reason: String?) {
            mutate(id) { it.copy(status = OrderStatus.CANCELED) }
        }

        override suspend fun completeOrder(id: Long) {
            mutate(id) { it.copy(status = OrderStatus.COMPLETED) }
        }

        override suspend fun refresh() = Unit

        override suspend fun getOrderById(id: Long): Order? = orders.firstOrNull { it.id == id }

        private fun mutate(id: Long, transform: (Order) -> Order) {
            val index = orders.indexOfFirst { it.id == id }
            if (index >= 0) {
                orders[index] = transform(orders[index])
            }
        }
    }


    private class TestCurrentUserProvider : CurrentUserProvider {
        private val currentUser = CurrentUser(id = "1", role = Role.LOADER)
        override fun observeCurrentUser(): Flow<CurrentUser> = flowOf(currentUser)
        override suspend fun getCurrentUser(): CurrentUser = currentUser
    }


    private fun testOrderDraft(): OrderDraft = OrderDraft(
        title = "Test",
        address = "Address",
        pricePerHour = 100.0,
        orderTime = OrderTime.Soon,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = 2,
        tags = emptyList(),
        meta = mapOf(Order.CREATED_AT_KEY to "0"),
        comment = null
    )

    private fun testOrder(id: Long, status: OrderStatus): Order {
        return Order(
            id = id,
            title = "Test",
            address = "Address",
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
