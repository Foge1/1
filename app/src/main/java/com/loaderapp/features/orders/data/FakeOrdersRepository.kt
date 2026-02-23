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

@Singleton
class FakeOrdersRepository @Inject constructor() : OrdersRepository {
    private val orders = MutableStateFlow<List<Order>>(emptyList())
    private val idGenerator = AtomicLong(1)

    override fun observeOrders(): Flow<List<Order>> = orders.asStateFlow()

    override suspend fun createOrder(order: Order) {
        simulateLatency()
        val resolvedId = if (order.id > 0) {
            idGenerator.updateAndGet { current -> maxOf(current, order.id + 1) }
            order.id
        } else {
            idGenerator.getAndIncrement()
        }

        val newOrder = order.copy(id = resolvedId, status = OrderStatus.STAFFING)
        orders.update { current -> current + newOrder }
    }

    override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) {
        simulateLatency()
        mutateOrder(orderId) { order ->
            val retained = order.applications.filterNot { it.loaderId == loaderId }
            order.copy(
                applications = retained + OrderApplication(
                    orderId = orderId,
                    loaderId = loaderId,
                    status = OrderApplicationStatus.APPLIED,
                    appliedAtMillis = now,
                    ratingSnapshot = null
                )
            )
        }
    }

    override suspend fun withdrawApplication(orderId: Long, loaderId: String) {
        simulateLatency()
        mutateOrder(orderId) { order ->
            order.copy(
                applications = order.applications.map {
                    if (it.loaderId == loaderId) it.copy(status = OrderApplicationStatus.WITHDRAWN) else it
                }
            )
        }
    }

    override suspend fun selectApplicant(orderId: Long, loaderId: String) {
        simulateLatency()
        mutateOrder(orderId) { order ->
            order.copy(
                applications = order.applications.map {
                    if (it.loaderId == loaderId) it.copy(status = OrderApplicationStatus.SELECTED) else it
                }
            )
        }
    }

    override suspend fun unselectApplicant(orderId: Long, loaderId: String) {
        simulateLatency()
        mutateOrder(orderId) { order ->
            order.copy(
                applications = order.applications.map {
                    if (it.loaderId == loaderId) it.copy(status = OrderApplicationStatus.APPLIED) else it
                }
            )
        }
    }

    override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
        simulateLatency()
        mutateOrder(orderId) { order ->
            val selected = order.applications.filter { it.status == OrderApplicationStatus.SELECTED }
            val assignments = selected.map {
                OrderAssignment(
                    orderId = orderId,
                    loaderId = it.loaderId,
                    status = OrderAssignmentStatus.ACTIVE,
                    assignedAtMillis = startedAtMillis,
                    startedAtMillis = startedAtMillis
                )
            }
            order.copy(
                status = OrderStatus.IN_PROGRESS,
                applications = order.applications.map {
                    if (it.status == OrderApplicationStatus.APPLIED) it.copy(status = OrderApplicationStatus.REJECTED) else it
                },
                assignments = assignments
            )
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        simulateLatency()
        mutateOrder(id) { order ->
            order.copy(
                status = OrderStatus.CANCELED,
                assignments = order.assignments.map {
                    if (it.status == OrderAssignmentStatus.ACTIVE) it.copy(status = OrderAssignmentStatus.CANCELED) else it
                }
            )
        }
    }

    override suspend fun completeOrder(id: Long) {
        simulateLatency()
        mutateOrder(id) { order ->
            order.copy(
                status = OrderStatus.COMPLETED,
                assignments = order.assignments.map {
                    if (it.status == OrderAssignmentStatus.ACTIVE) it.copy(status = OrderAssignmentStatus.COMPLETED) else it
                }
            )
        }
    }

    override suspend fun hasActiveAssignment(loaderId: String): Boolean {
        return orders.value.any { order -> order.assignments.any { it.loaderId == loaderId && it.status == OrderAssignmentStatus.ACTIVE } }
    }

    override suspend fun countActiveAppliedApplications(loaderId: String): Int {
        return orders.value.sumOf { order -> order.applications.count { it.loaderId == loaderId && it.status == OrderApplicationStatus.APPLIED } }
    }

    override suspend fun getOrderById(id: Long): Order? = orders.value.firstOrNull { it.id == id }

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

    private fun mutateOrder(id: Long, transform: (Order) -> Order) {
        orders.update { current ->
            current.map { order -> if (order.id == id) transform(order) else order }
        }
    }

    private suspend fun simulateLatency() {
        delay(Random.nextLong(150L, 401L))
    }

    private companion object {
        const val ORDER_EXPIRATION_GRACE_MS = 60_000L
    }
}
