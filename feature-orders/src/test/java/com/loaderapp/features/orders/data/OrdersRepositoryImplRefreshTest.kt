package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.data.local.dao.ApplicationsDao
import com.loaderapp.features.orders.data.local.dao.AssignmentsDao
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
import org.junit.Test

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

    @Test
    fun `InMemoryOrdersDao expireStaffingExactOrders updates only matching rows`() = runBlocking {
        val dao = InMemoryOrdersDao()
        dao.insertOrder(orderEntity(1L, OrderStatus.STAFFING.name, "exact", 10L))
        dao.insertOrder(orderEntity(2L, OrderStatus.STAFFING.name, "soon", null))
        dao.insertOrder(orderEntity(3L, OrderStatus.IN_PROGRESS.name, "exact", 10L))

        val updated = dao.expireStaffingExactOrders(
            staffingStatus = OrderStatus.STAFFING.name,
            expiredStatus = OrderStatus.EXPIRED.name,
            exactTimeType = "exact",
            expirationThreshold = 100L
        )

        assertEquals(1, updated)
        assertEquals(OrderStatus.EXPIRED.name, dao.getOrderById(1L)?.status)
        assertEquals(OrderStatus.STAFFING.name, dao.getOrderById(2L)?.status)
        assertEquals(OrderStatus.IN_PROGRESS.name, dao.getOrderById(3L)?.status)
    }

    @Test
    fun `InMemoryAssignmentsDao countAssignmentsByLoaderAndStatus works`() = runBlocking {
        val dao = InMemoryAssignmentsDao()
        dao.upsertAssignments(
            listOf(
                OrderAssignmentEntity(1L, "loader-1", OrderAssignmentStatus.ACTIVE.name, 100L, 100L),
                OrderAssignmentEntity(2L, "loader-1", OrderAssignmentStatus.ACTIVE.name, 200L, 200L),
                OrderAssignmentEntity(3L, "loader-1", OrderAssignmentStatus.COMPLETED.name, 300L, 300L),
            )
        )
        assertEquals(2, dao.countAssignmentsByLoaderAndStatus("loader-1", OrderAssignmentStatus.ACTIVE.name))
    }

    @Test
    fun `InMemoryApplicationsDao updateApplicationsStatusByOrder rejects APPLIED`() = runBlocking {
        val dao = InMemoryApplicationsDao()
        dao.upsertApplication(OrderApplicationEntity(1L, "loader-a", OrderApplicationStatus.APPLIED.name, 100L, null))
        dao.upsertApplication(OrderApplicationEntity(1L, "loader-b", OrderApplicationStatus.SELECTED.name, 101L, null))
        dao.updateApplicationsStatusByOrder(1L, OrderApplicationStatus.APPLIED.name, OrderApplicationStatus.REJECTED.name)
        assertEquals(OrderApplicationStatus.REJECTED.name, dao.getApplication(1L, "loader-a")!!.status)
        assertEquals(OrderApplicationStatus.SELECTED.name, dao.getApplication(1L, "loader-b")!!.status)
    }

    @Test
    fun `InMemoryApplicationsDao countActiveApplicationsForLimit ignores closed orders`() = runBlocking {
        val ordersDao = InMemoryOrdersDao().apply {
            insertOrder(orderEntity(1L, OrderStatus.STAFFING.name, "soon", null))
            insertOrder(orderEntity(2L, OrderStatus.IN_PROGRESS.name, "soon", null))
            insertOrder(orderEntity(3L, OrderStatus.COMPLETED.name, "soon", null))
            insertOrder(orderEntity(4L, OrderStatus.CANCELED.name, "soon", null))
        }
        val applicationsDao = InMemoryApplicationsDao { ordersDao.getOrders() }
        applicationsDao.upsertApplication(OrderApplicationEntity(1L, "loader-1", OrderApplicationStatus.APPLIED.name, 1L, null))
        applicationsDao.upsertApplication(OrderApplicationEntity(2L, "loader-1", OrderApplicationStatus.SELECTED.name, 2L, null))
        applicationsDao.upsertApplication(OrderApplicationEntity(3L, "loader-1", OrderApplicationStatus.APPLIED.name, 3L, null))
        applicationsDao.upsertApplication(OrderApplicationEntity(4L, "loader-1", OrderApplicationStatus.APPLIED.name, 4L, null))

        val count = applicationsDao.countActiveApplicationsForLimit(
            loaderId = "loader-1",
            applicationStatuses = listOf(OrderApplicationStatus.APPLIED.name, OrderApplicationStatus.SELECTED.name),
            activeOrderStatuses = OrderStatus.ACTIVE_FOR_APPLICATION_LIMIT.map { it.name }
        )

        assertEquals(2, count)
    }
}

private fun orderEntity(id: Long, status: String, orderTimeType: String, exactMillis: Long?): OrderEntity =
    OrderEntity(
        id = id,
        title = "title-$id",
        address = "address-$id",
        pricePerHour = 100.0,
        orderTimeType = orderTimeType,
        orderTimeExactMillis = exactMillis,
        durationMin = 60,
        workersCurrent = 0,
        workersTotal = 1,
        tags = emptyList(),
        meta = emptyMap(),
        comment = null,
        status = status,
        createdByUserId = "dispatcher"
    )

internal class InMemoryOrdersDao : OrdersDao {
    private val orders = MutableStateFlow<List<OrderEntity>>(emptyList())

    override fun observeOrders(): Flow<List<OrderEntity>> = orders
    override suspend fun getOrders(): List<OrderEntity> = orders.value
    override suspend fun getOrderById(id: Long): OrderEntity? = orders.value.firstOrNull { it.id == id }
    override suspend fun insertOrder(order: OrderEntity): Long {
        val newId = if (order.id > 0L) order.id else (orders.value.maxOfOrNull { it.id } ?: 0L) + 1L
        orders.update { current ->
            val existing = current.indexOfFirst { it.id == newId }
            if (existing >= 0) current.mapIndexed { i, e -> if (i == existing) order.copy(id = newId) else e }
            else current + order.copy(id = newId)
        }
        return newId
    }

    override suspend fun updateOrder(order: OrderEntity) {
        orders.update { it.map { e -> if (e.id == order.id) order else e } }
    }

    override suspend fun expireStaffingExactOrders(
        staffingStatus: String,
        expiredStatus: String,
        exactTimeType: String,
        expirationThreshold: Long
    ): Int {
        var updated = 0
        orders.update { list ->
            list.map { entity ->
                val shouldExpire = entity.status == staffingStatus &&
                    entity.orderTimeType == exactTimeType &&
                    entity.orderTimeExactMillis?.let { it < expirationThreshold } == true
                if (shouldExpire) {
                    updated++
                    entity.copy(status = expiredStatus)
                } else {
                    entity
                }
            }
        }
        return updated
    }
}

internal class InMemoryApplicationsDao(
    private val ordersProvider: suspend () -> List<OrderEntity> = { emptyList() }
) : ApplicationsDao {
    private val apps = MutableStateFlow<List<OrderApplicationEntity>>(emptyList())

    override fun observeApplications(): Flow<List<OrderApplicationEntity>> = apps
    override suspend fun getApplicationsByOrder(orderId: Long) = apps.value.filter { it.orderId == orderId }
    override suspend fun getApplication(orderId: Long, loaderId: String) =
        apps.value.firstOrNull { it.orderId == orderId && it.loaderId == loaderId }

    override suspend fun upsertApplication(application: OrderApplicationEntity) {
        apps.update { list ->
            val idx = list.indexOfFirst {
                it.orderId == application.orderId && it.loaderId == application.loaderId
            }
            if (idx >= 0) list.mapIndexed { i, a -> if (i == idx) application else a } else list + application
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

    override suspend fun countApplicationsByLoaderAndStatuses(loaderId: String, statuses: List<String>): Int =
        apps.value.count { it.loaderId == loaderId && it.status in statuses }

    override suspend fun countActiveApplicationsForLimit(
        loaderId: String,
        applicationStatuses: List<String>,
        activeOrderStatuses: List<String>
    ): Int {
        val activeOrderIds = ordersProvider()
            .asSequence()
            .filter { it.status in activeOrderStatuses }
            .map { it.id }
            .toSet()

        return apps.value.count {
            it.loaderId == loaderId &&
                it.status in applicationStatuses &&
                it.orderId in activeOrderIds
        }
    }
}

internal class InMemoryAssignmentsDao : AssignmentsDao {
    private val assigns = MutableStateFlow<List<OrderAssignmentEntity>>(emptyList())

    override fun observeAssignments(): Flow<List<OrderAssignmentEntity>> = assigns
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

    override suspend fun findActiveAssignmentsByLoaders(loaderIds: List<String>, status: String) =
        assigns.value
            .asSequence()
            .filter { it.loaderId in loaderIds && it.status == status }
            .map { com.loaderapp.features.orders.data.local.dao.LoaderOrderPair(loaderId = it.loaderId, orderId = it.orderId) }
            .toList()
    override suspend fun countAssignmentsByOrderLoaderAndStatuses(orderId: Long, loaderId: String, statuses: List<String>): Int =
        assigns.value.count { it.orderId == orderId && it.loaderId == loaderId && it.status in statuses }
}
