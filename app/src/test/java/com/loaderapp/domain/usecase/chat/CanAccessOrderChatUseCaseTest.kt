package com.loaderapp.domain.usecase.chat

import com.loaderapp.core.common.Result
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class CanAccessOrderChatUseCaseTest {

    @Test
    fun `returns error when order not found`() = runBlocking {
        val useCase = CanAccessOrderChatUseCase(FakeOrdersRepository(order = null))
        val result = useCase(CanAccessOrderChatParams(orderId = 100L, userId = 1L))
        assertEquals(Result.Error("Заказ не найден"), result)
    }

    @Test
    fun `returns true when order is in progress`() = runBlocking {
        val order = Order(
            id = 7L, title = "Order", address = "Addr", pricePerHour = 100.0,
            orderTime = OrderTime.Soon, durationMin = 60, workersCurrent = 1, workersTotal = 2,
            tags = emptyList(), meta = emptyMap(), comment = null,
            status = OrderStatus.IN_PROGRESS, createdByUserId = "dispatcher-1",
            acceptedByUserId = "loader-1", acceptedAtMillis = 10L
        )
        val useCase = CanAccessOrderChatUseCase(FakeOrdersRepository(order))
        val result = useCase(CanAccessOrderChatParams(orderId = 7L, userId = 1L))
        assertEquals(Result.Success(true), result)
    }

    private class FakeOrdersRepository(private val order: Order?) : OrdersRepository {
        override fun observeOrders(): Flow<List<Order>> = emptyFlow()
        override suspend fun createOrder(order: Order) = Unit
        @Deprecated("compat")
        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = order?.takeIf { it.id == id }
        override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) = Unit
        override suspend fun withdrawApplication(orderId: Long, loaderId: String) = Unit
        override suspend fun selectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun unselectApplicant(orderId: Long, loaderId: String) = Unit
        override suspend fun startOrder(orderId: Long, startedAtMillis: Long) = Unit
        override suspend fun hasActiveAssignment(loaderId: String): Boolean = false
        override suspend fun countActiveAppliedApplications(loaderId: String): Int = 0
    }
}
