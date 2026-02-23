package com.loaderapp.features.orders.data

import android.util.Log
import androidx.room.withTransaction
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.db.AppDatabase
import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
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
import kotlinx.coroutines.flow.map

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val ordersDao: OrdersDao
) : OrdersRepository {

    // ── Observation ────────────────────────────────────────────────────────────

    override fun observeOrders(): Flow<List<Order>> =
        combine(
            ordersDao.observeOrders(),
            ordersDao.observeApplications(),
            ordersDao.observeAssignments()
        ) { orderEntities, appEntities, assignmentEntities ->
            val appsByOrder = appEntities.groupBy { it.orderId }
            val assignsByOrder = assignmentEntities.groupBy { it.orderId }

            orderEntities.map { entity ->
                val apps = appsByOrder[entity.id]?.map { it.toDomain() } ?: emptyList()
                val assignments = assignsByOrder[entity.id]?.map { it.toDomain() } ?: emptyList()
                entity.toDomain(applications = apps, assignments = assignments)
            }
        }

    // ── Order lifecycle ────────────────────────────────────────────────────────

    override suspend fun createOrder(order: Order) {
        val newOrder = order.copy(
            id = 0L,
            status = OrderStatus.STAFFING,
            applications = emptyList(),
            assignments = emptyList()
        )
        val orderId = ordersDao.insertOrder(newOrder.toEntity())
        log("createOrder: id=$orderId, status=STAFFING")
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        db.withTransaction {
            val entity = ordersDao.getOrderById(id) ?: return@withTransaction
            ordersDao.updateOrder(entity.copy(status = OrderStatus.CANCELED.name))
            ordersDao.updateAssignmentsStatusByOrder(
                orderId = id,
                newStatus = OrderAssignmentStatus.CANCELED.name
            )
            log("cancelOrder: id=$id, ${entity.status}->CANCELED")
        }
    }

    override suspend fun completeOrder(id: Long) {
        db.withTransaction {
            val entity = ordersDao.getOrderById(id) ?: return@withTransaction
            ordersDao.updateOrder(entity.copy(status = OrderStatus.COMPLETED.name))
            ordersDao.updateAssignmentsStatusByOrder(
                orderId = id,
                newStatus = OrderAssignmentStatus.COMPLETED.name
            )
            log("completeOrder: id=$id, ${entity.status}->COMPLETED")
        }
    }

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val expirationThreshold = now - ORDER_EXPIRATION_GRACE_MS
        ordersDao.getOrders().forEach { entity ->
            val status = runCatching { OrderStatus.valueOf(entity.status) }.getOrNull()
            val orderTime = if (entity.orderTimeType == Order.TIME_TYPE_SOON) {
                OrderTime.Soon
            } else {
                OrderTime.Exact(entity.orderTimeExactMillis ?: 0L)
            }
            val dateTime = when (orderTime) {
                is OrderTime.Exact -> orderTime.dateTimeMillis
                OrderTime.Soon -> entity.meta["createdAt"]?.toLongOrNull() ?: now
            }
            val shouldExpire = status == OrderStatus.STAFFING &&
                orderTime is OrderTime.Exact &&
                dateTime < expirationThreshold

            if (shouldExpire) {
                ordersDao.updateOrder(entity.copy(status = OrderStatus.EXPIRED.name))
                log("refresh: expired id=${entity.id}")
            }
        }
    }

    override suspend fun getOrderById(id: Long): Order? {
        val entity = ordersDao.getOrderById(id) ?: return null
        val apps = ordersDao.getApplicationsByOrder(id).map { it.toDomain() }
        val assignments = ordersDao.getAssignmentsByOrder(id).map { it.toDomain() }
        return entity.toDomain(applications = apps, assignments = assignments)
    }

    // ── Application flow ───────────────────────────────────────────────────────

    override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) {
        val application = OrderApplicationEntity(
            orderId = orderId,
            loaderId = loaderId,
            status = OrderApplicationStatus.APPLIED.name,
            appliedAtMillis = now,
            ratingSnapshot = null
        )
        ordersDao.upsertApplication(application)
        log("applyToOrder: order=$orderId loader=$loaderId")
    }

    override suspend fun withdrawApplication(orderId: Long, loaderId: String) {
        val existing = ordersDao.getApplication(orderId, loaderId) ?: return
        val canWithdraw = existing.status == OrderApplicationStatus.APPLIED.name ||
            existing.status == OrderApplicationStatus.SELECTED.name
        if (canWithdraw) {
            ordersDao.updateApplicationStatus(
                orderId = orderId,
                loaderId = loaderId,
                newStatus = OrderApplicationStatus.WITHDRAWN.name
            )
            log("withdrawApplication: order=$orderId loader=$loaderId")
        }
    }

    // ── Selection flow ─────────────────────────────────────────────────────────

    override suspend fun selectApplicant(orderId: Long, loaderId: String) {
        db.withTransaction {
            ordersDao.updateApplicationStatus(
                orderId = orderId,
                loaderId = loaderId,
                newStatus = OrderApplicationStatus.SELECTED.name
            )
            log("selectApplicant: order=$orderId loader=$loaderId")
        }
    }

    override suspend fun unselectApplicant(orderId: Long, loaderId: String) {
        db.withTransaction {
            ordersDao.updateApplicationStatus(
                orderId = orderId,
                loaderId = loaderId,
                newStatus = OrderApplicationStatus.APPLIED.name
            )
            log("unselectApplicant: order=$orderId loader=$loaderId")
        }
    }

    // ── Start order ────────────────────────────────────────────────────────────

    override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
        db.withTransaction {
            val orderEntity = ordersDao.getOrderById(orderId)
                ?: error("startOrder: order $orderId not found")

            val selectedApplications = ordersDao.getApplicationsByOrder(orderId)
                .filter { it.status == OrderApplicationStatus.SELECTED.name }

            require(selectedApplications.isNotEmpty()) {
                "startOrder: no SELECTED applicants for order $orderId"
            }

            // Create ACTIVE assignment for each SELECTED loader
            val assignments = selectedApplications.map { app ->
                OrderAssignmentEntity(
                    orderId = orderId,
                    loaderId = app.loaderId,
                    status = OrderAssignmentStatus.ACTIVE.name,
                    assignedAtMillis = app.appliedAtMillis,
                    startedAtMillis = startedAtMillis
                )
            }
            ordersDao.upsertAssignments(assignments)

            // Reject all remaining APPLIED applicants
            ordersDao.updateApplicationsStatusByOrder(
                orderId = orderId,
                fromStatus = OrderApplicationStatus.APPLIED.name,
                toStatus = OrderApplicationStatus.REJECTED.name
            )

            // Transition order status to IN_PROGRESS
            ordersDao.updateOrder(orderEntity.copy(status = OrderStatus.IN_PROGRESS.name))

            log("startOrder: order=$orderId assignedLoaders=${selectedApplications.map { it.loaderId }}")
        }
    }

    // ── Invariant helpers ─────────────────────────────────────────────────────

    override suspend fun hasActiveAssignment(loaderId: String): Boolean =
        ordersDao.countAssignmentsByLoaderAndStatus(
            loaderId = loaderId,
            status = OrderAssignmentStatus.ACTIVE.name
        ) > 0

    override suspend fun countActiveAppliedApplications(loaderId: String): Int =
        ordersDao.countApplicationsByLoaderAndStatus(
            loaderId = loaderId,
            status = OrderApplicationStatus.APPLIED.name
        )


    // ── Private ───────────────────────────────────────────────────────────────

    private fun log(message: String) {
        Log.d(LOG_TAG, message)
    }

    private companion object {
        const val LOG_TAG = "OrdersRepo"
        const val ORDER_EXPIRATION_GRACE_MS = 60_000L
    }
}

