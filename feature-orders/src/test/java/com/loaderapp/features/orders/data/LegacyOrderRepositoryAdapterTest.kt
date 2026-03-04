package com.loaderapp.features.orders.data

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyOrderRepositoryAdapterTest {
    @Test
    fun `createOrder returns persisted id from feature repository`() =
        runTest {
            val repository = RecordingOrdersRepository(createdOrderId = 73L)
            val adapter = LegacyOrderRepositoryAdapter(ordersRepository = repository)

            val result = adapter.createOrder(validOrderModel())

            assertTrue(result is Result.Success)
            assertEquals(73L, (result as Result.Success).data)
            assertEquals(1, repository.createCalls)
        }

    private fun validOrderModel(): OrderModel =
        OrderModel(
            id = 0L,
            address = "Test address",
            dateTime = 1_700_000_000_000,
            cargoDescription = "Boxes",
            pricePerHour = 100.0,
            estimatedHours = 2,
            requiredWorkers = 2,
            minWorkerRating = 0f,
            status = OrderStatusModel.AVAILABLE,
            createdAt = 1_700_000_000_000,
            completedAt = null,
            workerId = null,
            dispatcherId = 55L,
            workerRating = null,
            comment = "",
            isAsap = false,
        )

    private class RecordingOrdersRepository(
        private val createdOrderId: Long,
    ) : OrdersRepository {
        var createCalls: Int = 0

        override fun observeOrders(): Flow<List<Order>> = flowOf(emptyList())

        override suspend fun createOrder(order: Order): Long {
            createCalls++
            return createdOrderId
        }

        override suspend fun cancelOrder(
            id: Long,
            reason: String?,
        ) = Unit

        override suspend fun completeOrder(id: Long) = Unit

        override suspend fun refresh() = Unit

        override suspend fun getOrderById(id: Long): Order? = null

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
}
