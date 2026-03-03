package com.loaderapp.domain.usecase

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.order.CreateOrderParams
import com.loaderapp.domain.usecase.order.CreateOrderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderUseCaseTest {
    @Test
    fun `createOrder returns success when repository succeeds`() =
        runTest(StandardTestDispatcher(testScheduler)) {
            val repository = FakeOrderRepository(createOrderResult = Result.Success(42L))
            val useCase = CreateOrderUseCase(repository)

            val result = useCase(CreateOrderParams(validOrder()))
            advanceUntilIdle()

            assertEquals(Result.Success(42L), result)
            assertEquals(1, repository.createOrderCalls)
        }

    @Test
    fun `createOrder returns error when repository returns error`() =
        runTest(StandardTestDispatcher(testScheduler)) {
            val repository = FakeOrderRepository(createOrderResult = Result.Error("db error"))
            val useCase = CreateOrderUseCase(repository)

            val result = useCase(CreateOrderParams(validOrder()))
            advanceUntilIdle()

            assertTrue(result is Result.Error)
            assertEquals("db error", (result as Result.Error).message)
            assertEquals(1, repository.createOrderCalls)
        }

    private fun validOrder(): OrderModel =
        OrderModel(
            id = 0,
            address = "Moscow, Lenina 1",
            dateTime = 1_700_000_000_000,
            cargoDescription = "Boxes",
            pricePerHour = 1500.0,
            estimatedHours = 2,
            requiredWorkers = 2,
            minWorkerRating = 0f,
            status = OrderStatusModel.AVAILABLE,
            createdAt = 1_700_000_000_000,
            completedAt = null,
            workerId = null,
            dispatcherId = 100,
            workerRating = null,
            comment = "",
            isAsap = false,
        )

    private class FakeOrderRepository(
        private val createOrderResult: Result<Long>,
    ) : OrderRepository {
        var createOrderCalls: Int = 0
            private set

        override fun getAllOrders(): Flow<List<OrderModel>> = emptyFlow()

        override fun getAvailableOrders(): Flow<List<OrderModel>> = emptyFlow()

        override fun getOrdersByWorker(workerId: Long): Flow<List<OrderModel>> = emptyFlow()

        override fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<OrderModel>> = emptyFlow()

        override suspend fun getOrderById(orderId: Long): Result<OrderModel> = Result.Error("not implemented")

        override fun getOrderByIdFlow(orderId: Long): Flow<OrderModel?> = emptyFlow()

        override fun searchOrders(
            query: String,
            status: OrderStatusModel?,
        ): Flow<List<OrderModel>> = emptyFlow()

        override fun searchOrdersByDispatcher(
            dispatcherId: Long,
            query: String,
        ): Flow<List<OrderModel>> = emptyFlow()

        override suspend fun createOrder(order: OrderModel): Result<Long> {
            createOrderCalls++
            return createOrderResult
        }

        override suspend fun updateOrder(order: OrderModel): Result<Unit> = Result.Error("not implemented")

        override suspend fun deleteOrder(order: OrderModel): Result<Unit> = Result.Error("not implemented")

        override suspend fun takeOrder(
            orderId: Long,
            workerId: Long,
        ): Result<Unit> = Result.Error("not implemented")

        override suspend fun completeOrder(orderId: Long): Result<Unit> = Result.Error("not implemented")

        override suspend fun cancelOrder(orderId: Long): Result<Unit> = Result.Error("not implemented")

        override suspend fun rateOrder(
            orderId: Long,
            rating: Float,
        ): Result<Unit> = Result.Error("not implemented")

        override fun getWorkerCountForOrder(orderId: Long): Flow<Int> = emptyFlow()

        override suspend fun getWorkerCountSync(orderId: Long): Int = 0

        override suspend fun hasWorkerTakenOrder(
            orderId: Long,
            workerId: Long,
        ): Boolean = false

        override fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> = emptyFlow()

        override fun getCompletedOrdersCount(workerId: Long): Flow<Int> = emptyFlow()

        override fun getTotalEarnings(workerId: Long): Flow<Double?> = emptyFlow()

        override fun getAverageRating(workerId: Long): Flow<Float?> = emptyFlow()

        override fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> = emptyFlow()

        override fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> = emptyFlow()
    }
}
