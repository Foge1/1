package com.loaderapp.domain.usecase

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.order.CreateOrderParams
import com.loaderapp.domain.usecase.order.CreateOrderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderUseCaseTest {
    @Test
    fun `createOrder returns success when repository succeeds`() =
        runTest(StandardTestDispatcher(testScheduler)) {
            val counter = CallsCounter()
            val repository = fakeOrderRepository(createOrderResult = Result.Success(42L), callsCounter = counter)
            val useCase = CreateOrderUseCase(repository)

            val result = useCase(CreateOrderParams(validOrder()))
            advanceUntilIdle()

            assertEquals(Result.Success(42L), result)
            assertEquals(1, counter.createOrderCalls)
        }

    @Test
    fun `createOrder returns error when repository returns error`() =
        runTest(StandardTestDispatcher(testScheduler)) {
            val counter = CallsCounter()
            val repository = fakeOrderRepository(createOrderResult = Result.Error("db error"), callsCounter = counter)
            val useCase = CreateOrderUseCase(repository)

            val result = useCase(CreateOrderParams(validOrder()))
            advanceUntilIdle()

            assertTrue(result is Result.Error)
            assertEquals("db error", (result as Result.Error).message)
            assertEquals(1, counter.createOrderCalls)
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
}

private class CallsCounter {
    var createOrderCalls: Int = 0
}

private fun fakeOrderRepository(
    createOrderResult: Result<Long>,
    callsCounter: CallsCounter,
): OrderRepository {
    val classLoader = OrderRepository::class.java.classLoader
    val interfaces = arrayOf(OrderRepository::class.java)

    return Proxy
        .newProxyInstance(classLoader, interfaces) { _, method, _ ->
            when (method.name) {
                "createOrder" -> {
                    callsCounter.createOrderCalls++
                    createOrderResult
                }

                "getOrderById" -> Result.Error("not implemented")
                "updateOrder",
                "deleteOrder",
                "takeOrder",
                "completeOrder",
                "cancelOrder",
                "rateOrder",
                -> Result.Error("not implemented")

                "getAllOrders",
                "getAvailableOrders",
                "getOrdersByWorker",
                "getOrdersByDispatcher",
                "getOrderByIdFlow",
                "searchOrders",
                "searchOrdersByDispatcher",
                "getWorkerCountForOrder",
                "getOrderIdsByWorker",
                "getCompletedOrdersCount",
                "getTotalEarnings",
                "getAverageRating",
                "getDispatcherCompletedCount",
                "getDispatcherActiveCount",
                -> emptyFlow<Any?>()

                "getWorkerCountSync" -> 0
                "hasWorkerTakenOrder" -> false
                else -> error("Unexpected method call: ${method.name}")
            }
        } as OrderRepository
}
