package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.local.entity.OrderEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for refresh and invariant helper methods.
 * Transaction-heavy operations (startOrder, cancelOrder, completeOrder) are tested
 * through FakeOrdersRepository, since Room withTransaction requires Robolectric/instrumented tests.
 */
class OrdersRepositoryImplRefreshTest {

    @Test
    fun `refresh does not expire soon orders`() = runBlocking {
        val repository = FakeOrdersRepository()
        repository.createOrder(
            Order(
                id = 0, title = "soon", address = "addr", pricePerHour = 100.0,
                orderTime = OrderTime.Soon, durationMin = 60, workersCurrent = 0,
                workersTotal = 1, tags = emptyList(), meta = emptyMap(),
                status = OrderStatus.STAFFING, createdByUserId = "2"
            )
        )
        repository.refresh()
        val status = repository.observeOrders().first().first().status
        assertEquals(OrderStatus.STAFFING, status)
    }

    @Test
    fun `refresh expires exact orders older than threshold`() = runBlocking {
        val repository = FakeOrdersRepository()
        repository.createOrder(
            Order(
                id = 0, title = "old", address = "addr", pricePerHour = 100.0,
                orderTime = OrderTime.Exact(System.currentTimeMillis() - 120_000),
                durationMin = 60, workersCurrent = 0, workersTotal = 1,
                tags = emptyList(), meta = emptyMap(), status = OrderStatus.STAFFING,
                createdByUserId = "2"
            )
        )
        repository.refresh()
        val status = repository.observeOrders().first().first().status
        assertEquals(OrderStatus.EXPIRED, status)
    }

    // ── InMemoryOrdersDao tests ───────────────────────────────────────────────

    @Test
    fun `InMemoryOrdersDao countAssignmentsByLoaderAndStatus works`() = runBlocking {
        val dao = InMemoryOrdersDao()
        dao.upsertAssignments(listOf(
            OrderAssignmentEntity(1L, "loader-1", OrderAssignmentStatus.ACTIVE.name, 100L, 100L),
            OrderAssignmentEntity(2L, "loader-1", OrderAssignmentStatus.ACTIVE.name, 200L, 200L),
            OrderAssignmentEntity(3L, "loader-1", OrderAssignmentStatus.COMPLETED.name, 300L, 300L),
        ))
        assertEquals(2, dao.countAssignmentsByLoaderAndStatus("loader-1", OrderAssignmentStatus.ACTIVE.name))
        assertEquals(0, dao.countAssignmentsByLoaderAndStatus("loader-2", OrderAssignmentStatus.ACTIVE.name))
    }

    @Test
    fun `InMemoryOrdersDao countApplicationsByLoaderAndStatus works`() = runBlocking {
        val dao = InMemoryOrdersDao()
        dao.upsertApplication(OrderApplicationEntity(1L, "loader-1", OrderApplicationStatus.APPLIED.name, 100L, null))
        dao.upsertApplication(OrderApplicationEntity(2L, "loader-1", OrderApplicationStatus.APPLIED.name, 200L, null))
        dao.upsertApplication(OrderApplicationEntity(3L, "loader-1", OrderApplicationStatus.WITHDRAWN.name, 300L, null))
        assertEquals(2, dao.countApplicationsByLoaderAndStatus("loader-1", OrderApplicationStatus.APPLIED.name))
    }

    @Test
    fun `InMemoryOrdersDao updateApplicationsStatusByOrder rejects APPLIED`() = runBlocking {
        val dao = InMemoryOrdersDao()
        dao.upsertApplication(OrderApplicationEntity(1L, "loader-a", OrderApplicationStatus.APPLIED.name, 100L, null))
        dao.upsertApplication(OrderApplicationEntity(1L, "loader-b", OrderApplicationStatus.SELECTED.name, 101L, null))
        dao.updateApplicationsStatusByOrder(1L, OrderApplicationStatus.APPLIED.name, OrderApplicationStatus.REJECTED.name)
        assertEquals(OrderApplicationStatus.REJECTED.name, dao.getApplication(1L, "loader-a")!!.status)
        // SELECTED should not be touched
        assertEquals(OrderApplicationStatus.SELECTED.name, dao.getApplication(1L, "loader-b")!!.status)
    }
}

/** In-memory OrdersDao for unit tests that don't need Room/Robolectric. */
internal class InMemoryOrdersDao : OrdersDao {
    private val orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    private val apps = MutableStateFlow<List<OrderApplicationEntity>>(emptyList())
    private val assigns = MutableStateFlow<List<OrderAssignmentEntity>>(emptyList())

    override fun observeOrders(): Flow<List<OrderEntity>> = orders
    override fun observeApplications(): Flow<List<OrderApplicationEntity>> = apps
    override fun observeAssignments(): Flow<List<OrderAssignmentEntity>> = assigns

    override suspend fun getOrders(): List<OrderEntity> = orders.value
    override suspend fun getOrderById(id: Long): OrderEntity? = orders.value.firstOrNull { it.id == id }
    override suspend fun insertOrder(order: OrderEntity): Long {
        val newId = (orders.value.maxOfOrNull { it.id } ?: 0L) + 1L
        orders.update { it + order.copy(id = newId) }
        return newId
    }
    override suspend fun updateOrder(order: OrderEntity) {
        orders.update { it.map { e -> if (e.id == order.id) order else e } }
    }
    override suspend fun getApplicationsByOrder(orderId: Long) = apps.value.filter { it.orderId == orderId }
    override suspend fun getApplication(orderId: Long, loaderId: String) =
        apps.value.firstOrNull { it.orderId == orderId && it.loaderId == loaderId }
    override suspend fun upsertApplication(application: OrderApplicationEntity) {
        apps.update { list ->
            val idx = list.indexOfFirst { it.orderId == application.orderId && it.loaderId == application.loaderId }
            if (idx >= 0) list.mapIndexed { i, a -> if (i == idx) application else a }
            else list + application
        }
    }
    override suspend fun updateApplicationStatus(orderId: Long, loaderId: String, newStatus: String) {
        apps.update { list ->
            list.map { a -> if (a.orderId == orderId && a.loaderId == loaderId) a.copy(status = newStatus) else a }
        }
    }
    override suspend fun updateApplicationsStatusByOrder(orderId: Long, fromStatus: String, toStatus: String) {
        apps.update { list ->
            list.map { a -> if (a.orderId == orderId && a.status == fromStatus) a.copy(status = toStatus) else a }
        }
    }
    override suspend fun countApplicationsByLoaderAndStatus(loaderId: String, status: String): Int =
        apps.value.count { it.loaderId == loaderId && it.status == status }
    override suspend fun getAssignmentsByOrder(orderId: Long) = assigns.value.filter { it.orderId == orderId }
    override suspend fun upsertAssignments(assignments: List<OrderAssignmentEntity>) {
        assigns.update { list ->
            val current = list.toMutableList()
            assignments.forEach { a ->
                val idx = current.indexOfFirst { it.orderId == a.orderId && it.loaderId == a.loaderId }
                if (idx >= 0) current[idx] = a else current.add(a)
            }
            current
        }
    }
    override suspend fun updateAssignmentsStatusByOrder(orderId: Long, newStatus: String) {
        assigns.update { list -> list.map { a -> if (a.orderId == orderId) a.copy(status = newStatus) else a } }
    }
    override suspend fun countAssignmentsByLoaderAndStatus(loaderId: String, status: String): Int =
        assigns.value.count { it.loaderId == loaderId && it.status == status }
}
