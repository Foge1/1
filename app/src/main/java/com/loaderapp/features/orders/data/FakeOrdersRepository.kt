package com.loaderapp.features.orders.data

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignment
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class FakeOrdersRepository @Inject constructor() : OrdersRepository {

    private val mutex = Mutex()
    private val orders = MutableStateFlow<List<Order>>(emptyList())
    private val applications = MutableStateFlow<List<OrderApplication>>(emptyList())
    private val assignments = MutableStateFlow<List<OrderAssignment>>(emptyList())
    private val idGenerator = AtomicLong(1)

    override fun observeOrders(): Flow<List<Order>> {
        // Return orders enriched with applications and assignments
        return kotlinx.coroutines.flow.combine(
            orders.asStateFlow(),
            applications.asStateFlow(),
            assignments.asStateFlow()
        ) { orderList, appList, assignList ->
            val appsByOrder = appList.groupBy { it.orderId }
            val assignByOrder = assignList.groupBy { it.orderId }
            orderList.map { order ->
                order.copy(
                    applications = appsByOrder[order.id] ?: emptyList(),
                    assignments = assignByOrder[order.id] ?: emptyList()
                )
            }
        }
    }

    override suspend fun createOrder(order: Order) {
        simulateLatency()
        val resolvedId = if (order.id > 0) {
            idGenerator.updateAndGet { current -> maxOf(current, order.id + 1) }
            order.id
        } else {
            idGenerator.getAndIncrement()
        }

        val newOrder = order.copy(
            id = resolvedId,
            status = OrderStatus.STAFFING,
            applications = emptyList(),
            assignments = emptyList()
        )
        orders.update { it + newOrder }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        simulateLatency()
        mutex.withLock {
            mutateOrder(id) { it.copy(status = OrderStatus.CANCELED) }
            assignments.update { list ->
                list.map { a ->
                    if (a.orderId == id && a.status == OrderAssignmentStatus.ACTIVE) {
                        a.copy(status = OrderAssignmentStatus.CANCELED)
                    } else a
                }
            }
        }
    }

    override suspend fun completeOrder(id: Long) {
        simulateLatency()
        mutex.withLock {
            mutateOrder(id) { it.copy(status = OrderStatus.COMPLETED) }
            assignments.update { list ->
                list.map { a ->
                    if (a.orderId == id && a.status == OrderAssignmentStatus.ACTIVE) {
                        a.copy(status = OrderAssignmentStatus.COMPLETED)
                    } else a
                }
            }
        }
    }

    override suspend fun getOrderById(id: Long): Order? {
        val order = orders.value.firstOrNull { it.id == id } ?: return null
        return order.copy(
            applications = applications.value.filter { it.orderId == id },
            assignments = assignments.value.filter { it.orderId == id }
        )
    }

    override suspend fun refresh() {
        simulateLatency()
        val now = System.currentTimeMillis()
        val expirationThreshold = now - ORDER_EXPIRATION_GRACE_MS
        orders.update { current ->
            current.map { order ->
                val shouldExpire = order.status == OrderStatus.STAFFING &&
                    order.orderTime is OrderTime.Exact &&
                    order.dateTime < expirationThreshold
                if (shouldExpire) order.copy(status = OrderStatus.EXPIRED) else order
            }
        }
    }

    // ── Application flow ───────────────────────────────────────────────────────

    override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) {
        simulateLatency()
        val application = OrderApplication(
            orderId = orderId,
            loaderId = loaderId,
            status = OrderApplicationStatus.APPLIED,
            appliedAtMillis = now,
            ratingSnapshot = null
        )
        applications.update { list ->
            val existing = list.indexOfFirst { it.orderId == orderId && it.loaderId == loaderId }
            if (existing >= 0) list.mapIndexed { i, a -> if (i == existing) application else a }
            else list + application
        }
    }

    override suspend fun withdrawApplication(orderId: Long, loaderId: String) {
        simulateLatency()
        applications.update { list ->
            list.map { a ->
                if (a.orderId == orderId && a.loaderId == loaderId &&
                    (a.status == OrderApplicationStatus.APPLIED || a.status == OrderApplicationStatus.SELECTED)
                ) {
                    a.copy(status = OrderApplicationStatus.WITHDRAWN)
                } else a
            }
        }
    }

    override suspend fun selectApplicant(orderId: Long, loaderId: String) {
        simulateLatency()
        applications.update { list ->
            list.map { a ->
                if (a.orderId == orderId && a.loaderId == loaderId) a.copy(status = OrderApplicationStatus.SELECTED)
                else a
            }
        }
    }

    override suspend fun unselectApplicant(orderId: Long, loaderId: String) {
        simulateLatency()
        applications.update { list ->
            list.map { a ->
                if (a.orderId == orderId && a.loaderId == loaderId && a.status == OrderApplicationStatus.SELECTED) {
                    a.copy(status = OrderApplicationStatus.APPLIED)
                } else a
            }
        }
    }

    override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
        simulateLatency()
        mutex.withLock {
            val selected = applications.value.filter {
                it.orderId == orderId && it.status == OrderApplicationStatus.SELECTED
            }
            require(selected.isNotEmpty()) { "startOrder: no SELECTED applicants for order $orderId" }

            // Create ACTIVE assignments
            val newAssignments = selected.map { app ->
                OrderAssignment(
                    orderId = orderId,
                    loaderId = app.loaderId,
                    status = OrderAssignmentStatus.ACTIVE,
                    assignedAtMillis = startedAtMillis,
                    startedAtMillis = startedAtMillis
                )
            }
            assignments.update { it + newAssignments }

            // Reject remaining APPLIED
            applications.update { list ->
                list.map { a ->
                    if (a.orderId == orderId && a.status == OrderApplicationStatus.APPLIED) {
                        a.copy(status = OrderApplicationStatus.REJECTED)
                    } else a
                }
            }

            // Order → IN_PROGRESS
            mutateOrder(orderId) { it.copy(status = OrderStatus.IN_PROGRESS) }
        }
    }

    override suspend fun hasActiveAssignment(loaderId: String): Boolean =
        assignments.value.any { it.loaderId == loaderId && it.status == OrderAssignmentStatus.ACTIVE }

    override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean =
        assignments.value.any {
            it.orderId == orderId &&
                it.loaderId == loaderId &&
                it.status == OrderAssignmentStatus.ACTIVE
        }

    override suspend fun countActiveApplicationsForLimit(loaderId: String): Int =
        applications.value.count { application ->
            val hasActiveStatus = application.status == OrderApplicationStatus.APPLIED ||
                application.status == OrderApplicationStatus.SELECTED
            val orderStatus = orders.value.firstOrNull { it.id == application.orderId }?.status
            application.loaderId == loaderId &&
                hasActiveStatus &&
                orderStatus in OrderStatus.ACTIVE_FOR_APPLICATION_LIMIT
        }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun mutateOrder(id: Long, transform: (Order) -> Order) {
        orders.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            current.mapIndexed { i, order -> if (i == index) transform(order) else order }
        }
    }

    private suspend fun simulateLatency() {
        delay(Random.nextLong(150L, 401L))
    }

    private companion object {
        const val ORDER_EXPIRATION_GRACE_MS = 60_000L
    }
}
