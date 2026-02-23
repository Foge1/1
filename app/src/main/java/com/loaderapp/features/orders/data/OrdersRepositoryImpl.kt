package com.loaderapp.features.orders.data

import android.util.Log
import androidx.room.withTransaction
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.db.AppDatabase
import com.loaderapp.features.orders.data.mappers.toDomain
import com.loaderapp.features.orders.data.mappers.toEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignment
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val ordersDao: OrdersDao
) : OrdersRepository {

    override fun observeOrders(): Flow<List<Order>> =
        combine(
            ordersDao.observeOrders(),
            ordersDao.observeApplications(),
            ordersDao.observeAssignments()
        ) { orders, applications, assignments ->
            val applicationsByOrder = applications.map { it.toDomain() }.groupBy { it.orderId }
            val assignmentsByOrder = assignments.map { it.toDomain() }.groupBy { it.orderId }

            orders.map { entity ->
                entity.toDomain(
                    applications = applicationsByOrder[entity.id].orEmpty(),
                    assignments = assignmentsByOrder[entity.id].orEmpty()
                )
            }
        }

    override suspend fun createOrder(order: Order) {
        val createdOrder = order.copy(id = 0L, status = OrderStatus.STAFFING)
        val orderId = ordersDao.insertOrder(createdOrder.toEntity())
        logDebug(action = "createOrder", orderId = orderId, oldStatus = null, newStatus = createdOrder.status)
    }

    override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) {
        ordersDao.upsertApplication(
            OrderApplication(
                orderId = orderId,
                loaderId = loaderId,
                status = OrderApplicationStatus.APPLIED,
                appliedAtMillis = now,
                ratingSnapshot = null
            ).toEntity()
        )
    }

    override suspend fun withdrawApplication(orderId: Long, loaderId: String) {
        val existing = ordersDao.getApplication(orderId = orderId, loaderId = loaderId) ?: return
        if (existing.status != OrderApplicationStatus.WITHDRAWN.name) {
            ordersDao.updateApplicationStatus(orderId, loaderId, OrderApplicationStatus.WITHDRAWN.name)
        }
    }

    override suspend fun selectApplicant(orderId: Long, loaderId: String) {
        database.withTransaction {
            ordersDao.updateApplicationStatus(orderId, loaderId, OrderApplicationStatus.SELECTED.name)
        }
    }

    override suspend fun unselectApplicant(orderId: Long, loaderId: String) {
        database.withTransaction {
            ordersDao.updateApplicationStatus(orderId, loaderId, OrderApplicationStatus.APPLIED.name)
        }
    }

    override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
        database.withTransaction {
            val orderEntity = ordersDao.getOrderById(orderId) ?: return@withTransaction
            val applications = ordersDao.getApplicationsByOrder(orderId).map { it.toDomain() }
            val selectedApplications = applications.filter { it.status == OrderApplicationStatus.SELECTED }

            if (selectedApplications.isNotEmpty()) {
                val assignments = selectedApplications.map { application ->
                    OrderAssignment(
                        orderId = orderId,
                        loaderId = application.loaderId,
                        status = OrderAssignmentStatus.ACTIVE,
                        assignedAtMillis = startedAtMillis,
                        startedAtMillis = startedAtMillis
                    ).toEntity()
                }
                ordersDao.upsertAssignments(assignments)
            }

            ordersDao.updateApplicationsStatusByOrder(
                orderId = orderId,
                fromStatus = OrderApplicationStatus.APPLIED.name,
                toStatus = OrderApplicationStatus.REJECTED.name
            )
            ordersDao.updateOrder(orderEntity.toDomain().copy(status = OrderStatus.IN_PROGRESS).toEntity())
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        database.withTransaction {
            mutateOrderStatus(action = "cancelOrder", id = id, newStatus = OrderStatus.CANCELED)
            ordersDao.updateAssignmentsStatusByOrder(id, OrderAssignmentStatus.CANCELED.name)
        }
    }

    override suspend fun completeOrder(id: Long) {
        database.withTransaction {
            mutateOrderStatus(action = "completeOrder", id = id, newStatus = OrderStatus.COMPLETED)
            ordersDao.updateAssignmentsStatusByOrder(id, OrderAssignmentStatus.COMPLETED.name)
        }
    }

    override suspend fun hasActiveAssignment(loaderId: String): Boolean {
        return ordersDao.countAssignmentsByLoaderAndStatus(loaderId, OrderAssignmentStatus.ACTIVE.name) > 0
    }

    override suspend fun countActiveAppliedApplications(loaderId: String): Int {
        return ordersDao.countApplicationsByLoaderAndStatus(loaderId, OrderApplicationStatus.APPLIED.name)
    }

    override suspend fun getOrderById(id: Long): Order? {
        val order = ordersDao.getOrderById(id) ?: return null
        return order.toDomain(
            applications = ordersDao.getApplicationsByOrder(id).map { it.toDomain() },
            assignments = ordersDao.getAssignmentsByOrder(id).map { it.toDomain() }
        )
    }

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val expirationThreshold = now - ORDER_EXPIRATION_GRACE_MS
        ordersDao.getOrders().forEach { entity ->
            val order = entity.toDomain()
            val shouldExpire = order.status == OrderStatus.STAFFING &&
                order.orderTime is OrderTime.Exact &&
                order.dateTime < expirationThreshold

            if (shouldExpire) {
                val expired = order.copy(status = OrderStatus.EXPIRED)
                ordersDao.updateOrder(expired.toEntity())
                logDebug(action = "refresh", orderId = order.id, oldStatus = order.status, newStatus = expired.status)
            }
        }
    }

    private suspend fun mutateOrderStatus(action: String, id: Long, newStatus: OrderStatus) {
        val current = ordersDao.getOrderById(id)?.toDomain() ?: return
        val updated = current.copy(status = newStatus)
        ordersDao.updateOrder(updated.toEntity())
        logDebug(action = action, orderId = id, oldStatus = current.status, newStatus = updated.status)
    }

    private fun logDebug(action: String, orderId: Long, oldStatus: OrderStatus?, newStatus: OrderStatus) {
        Log.d(LOG_TAG, "$action: id=$orderId, ${oldStatus ?: "NONE"}->$newStatus")
    }

    private companion object {
        const val LOG_TAG = "OrdersRepo"
        const val ORDER_EXPIRATION_GRACE_MS = 60_000L
    }
}
