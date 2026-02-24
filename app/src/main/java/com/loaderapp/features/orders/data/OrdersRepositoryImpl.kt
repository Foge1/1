package com.loaderapp.features.orders.data

import android.util.Log
import androidx.room.withTransaction
import com.loaderapp.features.orders.data.local.dao.ApplicationsDao
import com.loaderapp.features.orders.data.local.dao.AssignmentsDao
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.local.db.OrdersDatabase
import com.loaderapp.features.orders.data.local.entity.OrderApplicationEntity
import com.loaderapp.features.orders.data.local.entity.OrderAssignmentEntity
import com.loaderapp.features.orders.data.mappers.OrdersGraphMapper
import com.loaderapp.features.orders.data.mappers.toDomain
import com.loaderapp.features.orders.data.mappers.toEntity
import com.loaderapp.features.orders.data.mappers.toPersistedValue
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val db: OrdersDatabase,
    private val ordersDao: OrdersDao,
    private val applicationsDao: ApplicationsDao,
    private val assignmentsDao: AssignmentsDao,
    private val ordersGraphMapper: OrdersGraphMapper,
) : OrdersRepository {

    override fun observeOrders(): Flow<List<Order>> =
        combine(
            ordersDao.observeOrders(),
            applicationsDao.observeApplications(),
            assignmentsDao.observeAssignments()
        ) { orderEntities, appEntities, assignmentEntities ->
            ordersGraphMapper.toDomainOrders(orderEntities, appEntities, assignmentEntities)
        }

    override suspend fun createOrder(order: Order) {
        val newOrder = order.copy(
            id = 0L,
            status = OrderStatus.STAFFING,
            applications = emptyList(),
            assignments = emptyList()
        )
        val orderId = ordersDao.insertOrder(newOrder.toEntity())
        log("createOrder: id=$orderId, status=${newOrder.status}")
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        db.withTransaction {
            val entity = ordersDao.getOrderById(id) ?: return@withTransaction
            ordersDao.updateOrder(entity.copy(status = OrderStatus.CANCELED.toPersistedValue()))
            assignmentsDao.updateAssignmentsStatusByOrder(orderId = id, newStatus = OrderAssignmentStatus.CANCELED)
            log("cancelOrder: id=$id, ${entity.status}->CANCELED")
        }
    }

    override suspend fun completeOrder(id: Long) {
        db.withTransaction {
            val entity = ordersDao.getOrderById(id) ?: return@withTransaction
            ordersDao.updateOrder(entity.copy(status = OrderStatus.COMPLETED.toPersistedValue()))
            assignmentsDao.updateAssignmentsStatusByOrder(orderId = id, newStatus = OrderAssignmentStatus.COMPLETED)
            log("completeOrder: id=$id, ${entity.status}->COMPLETED")
        }
    }

    override suspend fun refresh() {
        val expirationThreshold = System.currentTimeMillis() - ORDER_EXPIRATION_GRACE_MS
        val expiredCount = ordersDao.expireStaffingExactOrders(
            staffingStatus = OrderStatus.STAFFING.toPersistedValue(),
            expiredStatus = OrderStatus.EXPIRED.toPersistedValue(),
            exactTimeType = TIME_TYPE_EXACT,
            expirationThreshold = expirationThreshold
        )
        if (expiredCount > 0) {
            log("refresh: expired=$expiredCount")
        }
    }

    override suspend fun getOrderById(id: Long): Order? {
        val entity = ordersDao.getOrderById(id) ?: return null
        val apps = applicationsDao.getApplicationsByOrder(id).map { it.toDomain() }
        val assignments = assignmentsDao.getAssignmentsByOrder(id).map { it.toDomain() }
        return entity.toDomain(applications = apps, assignments = assignments)
    }

    override suspend fun applyToOrder(orderId: Long, loaderId: String, now: Long) {
        db.withTransaction {
            val order = ordersDao.getOrderById(orderId) ?: return@withTransaction
            if (order.status != OrderStatus.STAFFING.toPersistedValue()) {
                log("applyToOrder: skipped order=$orderId status=${order.status}")
                return@withTransaction
            }

            val existing = applicationsDao.getApplication(orderId, loaderId)
            if (existing != null) {
                log("applyToOrder: idempotent hit order=$orderId loader=$loaderId")
                return@withTransaction
            }

            applicationsDao.upsertApplication(
                OrderApplicationEntity(
                    orderId = orderId,
                    loaderId = loaderId,
                    status = OrderApplicationStatus.APPLIED.toPersistedValue(),
                    appliedAtMillis = now,
                    ratingSnapshot = null
                )
            )
            log("applyToOrder: order=$orderId loader=$loaderId")
        }
    }

    override suspend fun withdrawApplication(orderId: Long, loaderId: String) {
        val existing = applicationsDao.getApplication(orderId, loaderId) ?: return
        val canWithdraw = existing.status in setOf(
            OrderApplicationStatus.APPLIED.toPersistedValue(),
            OrderApplicationStatus.SELECTED.toPersistedValue()
        )
        if (canWithdraw) {
            applicationsDao.updateApplicationStatus(
                orderId = orderId,
                loaderId = loaderId,
                newStatus = OrderApplicationStatus.WITHDRAWN
            )
            log("withdrawApplication: order=$orderId loader=$loaderId")
        }
    }

    override suspend fun selectApplicant(orderId: Long, loaderId: String) {
        db.withTransaction {
            applicationsDao.updateApplicationStatus(
                orderId = orderId,
                loaderId = loaderId,
                newStatus = OrderApplicationStatus.SELECTED
            )
            log("selectApplicant: order=$orderId loader=$loaderId")
        }
    }

    override suspend fun unselectApplicant(orderId: Long, loaderId: String) {
        db.withTransaction {
            applicationsDao.updateApplicationStatus(
                orderId = orderId,
                loaderId = loaderId,
                newStatus = OrderApplicationStatus.APPLIED
            )
            log("unselectApplicant: order=$orderId loader=$loaderId")
        }
    }

    override suspend fun startOrder(orderId: Long, startedAtMillis: Long) {
        db.withTransaction {
            val orderEntity = ordersDao.getOrderById(orderId)
                ?: error("startOrder: order $orderId not found")

            val selectedApplications = applicationsDao.getApplicationsByOrder(orderId)
                .filter { it.status == OrderApplicationStatus.SELECTED.toPersistedValue() }

            require(selectedApplications.isNotEmpty()) {
                "startOrder: no SELECTED applicants for order $orderId"
            }

            val assignments = selectedApplications.map { app ->
                OrderAssignmentEntity(
                    orderId = orderId,
                    loaderId = app.loaderId,
                    status = OrderAssignmentStatus.ACTIVE.toPersistedValue(),
                    assignedAtMillis = app.appliedAtMillis,
                    startedAtMillis = startedAtMillis
                )
            }
            assignmentsDao.upsertAssignments(assignments)

            applicationsDao.updateApplicationsStatusByOrder(
                orderId = orderId,
                fromStatus = OrderApplicationStatus.APPLIED,
                toStatus = OrderApplicationStatus.REJECTED
            )

            ordersDao.updateOrder(orderEntity.copy(status = OrderStatus.IN_PROGRESS.toPersistedValue()))

            log("startOrder: order=$orderId assignedLoaders=${selectedApplications.map { it.loaderId }}")
        }
    }

    override suspend fun hasActiveAssignment(loaderId: String): Boolean =
        assignmentsDao.countAssignmentsByLoaderAndStatus(
            loaderId = loaderId,
            status = OrderAssignmentStatus.ACTIVE
        ) > 0

    override suspend fun hasActiveAssignmentInOrder(orderId: Long, loaderId: String): Boolean =
        assignmentsDao.countAssignmentsByOrderLoaderAndStatuses(
            orderId = orderId,
            loaderId = loaderId,
            statuses = listOf(OrderAssignmentStatus.ACTIVE)
        ) > 0

    override suspend fun countActiveApplicationsForLimit(loaderId: String): Int =
        applicationsDao.countApplicationsByLoaderAndStatuses(
            loaderId = loaderId,
            statuses = listOf(OrderApplicationStatus.APPLIED, OrderApplicationStatus.SELECTED)
        )

    private fun log(message: String) {
        Log.d(LOG_TAG, message)
    }

    private companion object {
        const val LOG_TAG = "OrdersRepo"
        const val ORDER_EXPIRATION_GRACE_MS = 60_000L
        const val TIME_TYPE_EXACT = "exact"
    }
}

private suspend fun ApplicationsDao.updateApplicationStatus(
    orderId: Long,
    loaderId: String,
    newStatus: OrderApplicationStatus
) {
    updateApplicationStatus(orderId = orderId, loaderId = loaderId, newStatus = newStatus.toPersistedValue())
}

private suspend fun ApplicationsDao.updateApplicationsStatusByOrder(
    orderId: Long,
    fromStatus: OrderApplicationStatus,
    toStatus: OrderApplicationStatus,
) {
    updateApplicationsStatusByOrder(
        orderId = orderId,
        fromStatus = fromStatus.toPersistedValue(),
        toStatus = toStatus.toPersistedValue()
    )
}

private suspend fun ApplicationsDao.countApplicationsByLoaderAndStatuses(
    loaderId: String,
    statuses: List<OrderApplicationStatus>
): Int = countApplicationsByLoaderAndStatuses(loaderId, statuses.map { it.toPersistedValue() })

private suspend fun AssignmentsDao.updateAssignmentsStatusByOrder(orderId: Long, newStatus: OrderAssignmentStatus) {
    updateAssignmentsStatusByOrder(orderId = orderId, newStatus = newStatus.toPersistedValue())
}

private suspend fun AssignmentsDao.countAssignmentsByLoaderAndStatus(
    loaderId: String,
    status: OrderAssignmentStatus
): Int = countAssignmentsByLoaderAndStatus(loaderId = loaderId, status = status.toPersistedValue())


private suspend fun AssignmentsDao.countAssignmentsByOrderLoaderAndStatuses(
    orderId: Long,
    loaderId: String,
    statuses: List<OrderAssignmentStatus>
): Int = countAssignmentsByOrderLoaderAndStatuses(
    orderId = orderId,
    loaderId = loaderId,
    statuses = statuses.map { it.toPersistedValue() }
)
