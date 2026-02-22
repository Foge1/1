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
import org.junit.Assert.assertTrue
import org.junit.Test

class CanAccessOrderChatUseCaseTest {

    @Test
    fun `returns false when order not found`() = runBlocking {
        val useCase = CanAccessOrderChatUseCase(FakeOrdersRepository(order = null))

        val result = useCase(CanAccessOrderChatParams(orderId = 100L, userId = 1L))

        assertTrue(result is Result.Success)
        assertEquals(false, (result as Result.Success).data)
    }

    @Test
    fun `returns true when order is in progress`() = runBlocking {
        val order = Order(
            id = 7L,
            title = "Order",
            address = "Addr",
            pricePerHour = 100.0,
            orderTime = OrderTime.Soon,
            durationMin = 60,
            workersCurrent = 1,
            workersTotal = 2,
            tags = emptyList(),
            meta = emptyMap(),
            comment = null,
            status = OrderStatus.IN_PROGRESS,
            createdByUserId = "dispatcher-1",
            acceptedByUserId = "loader-1",
            acceptedAtMillis = 10L
        )
        val useCase = CanAccessOrderChatUseCase(FakeOrdersRepository(order))

        val result = useCase(CanAccessOrderChatParams(orderId = 7L, userId = 1L))

        assertEquals(Result.Success(true), result)
    }

    private class FakeOrdersRepository(private val order: Order?) : OrdersRepository {
        override fun observeOrders(): Flow<List<Order>> = emptyFlow()
        override suspend fun createOrder(order: Order) = Unit
        override suspend fun acceptOrder(id: Long, acceptedByUserId: String, acceptedAtMillis: Long) = Unit
        override suspend fun cancelOrder(id: Long, reason: String?) = Unit
        override suspend fun completeOrder(id: Long) = Unit
        override suspend fun refresh() = Unit
        override suspend fun getOrderById(id: Long): Order? = order?.takeIf { it.id == id }
    }
}
