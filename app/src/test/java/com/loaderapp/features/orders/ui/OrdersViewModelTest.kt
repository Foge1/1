package com.loaderapp.features.orders.ui

import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.features.orders.data.FakeOrdersRepository
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersViewModelTest {
    @Test
    fun `legacy status mapper maps staffing to available`() = runBlocking {
        val order = Order(
            id = 1,
            title = "t",
            address = "a",
            pricePerHour = 1.0,
            orderTime = OrderTime.Soon,
            durationMin = 60,
            workersCurrent = 0,
            workersTotal = 1,
            tags = emptyList(),
            meta = emptyMap(),
            status = OrderStatus.STAFFING,
            createdByUserId = "d"
        )
        val repo = FakeOrdersRepository()
        repo.createOrder(order)

        assertEquals(OrderStatusModel.AVAILABLE, repo.observeOrders().first().first().toOrderModel().status)
    }
}
