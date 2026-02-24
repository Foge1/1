package com.loaderapp.features.orders.data.mappers

import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersGraphMapperTest {

    private val mapper = OrdersGraphMapper()

    @Test
    fun `maps orders with linked applications and assignments`() {
        val orders = listOf(orderEntity(id = 1L), orderEntity(id = 2L))
        val apps = listOf(
            appEntity(orderId = 1L, loaderId = "loader-1"),
            appEntity(orderId = 1L, loaderId = "loader-2")
        )
        val assignments = listOf(assignEntity(orderId = 2L, loaderId = "loader-3"))

        val mapped = mapper.toDomainOrders(orders, apps, assignments)

        assertEquals(2, mapped.size)
        assertEquals(2, mapped.first { it.id == 1L }.applications.size)
        assertEquals(1, mapped.first { it.id == 2L }.assignments.size)
    }

    @Test
    fun `maps empty related lists without crashes`() {
        val mapped = mapper.toDomainOrders(listOf(orderEntity(10L)), emptyList(), emptyList())

        assertEquals(1, mapped.size)
        assertTrue(mapped.single().applications.isEmpty())
        assertTrue(mapped.single().assignments.isEmpty())
    }

    @Test
    fun `ignores foreign related entities from other orders`() {
        val mapped = mapper.toDomainOrders(
            orderEntities = listOf(orderEntity(5L)),
            applicationEntities = listOf(appEntity(orderId = 6L, loaderId = "other")),
            assignmentEntities = listOf(assignEntity(orderId = 7L, loaderId = "other"))
        )

        assertTrue(mapped.single().applications.isEmpty())
        assertTrue(mapped.single().assignments.isEmpty())
    }

    private fun orderEntity(id: Long) = OrderEntity(
        id = id,
        title = "title-$id",
        address = "address",
        pricePerHour = 100.0,
        orderTimeType = "soon",
        orderTimeExactMillis = null,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = 2,
        tags = emptyList(),
        meta = emptyMap(),
        comment = null,
        status = "STAFFING",
        createdByUserId = "dispatcher"
    )

    private fun appEntity(orderId: Long, loaderId: String) = OrderApplicationEntity(
        orderId = orderId,
        loaderId = loaderId,
        status = "APPLIED",
        appliedAtMillis = 0L,
        ratingSnapshot = null
    )

    private fun assignEntity(orderId: Long, loaderId: String) = OrderAssignmentEntity(
        orderId = orderId,
        loaderId = loaderId,
        status = "ACTIVE",
        assignedAtMillis = 0L,
        startedAtMillis = null
    )
}
