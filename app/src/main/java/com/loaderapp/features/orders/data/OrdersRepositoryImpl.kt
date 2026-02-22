package com.loaderapp.features.orders.data

import android.util.Log
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.mappers.toDomain
import com.loaderapp.features.orders.data.mappers.toEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val ordersDao: OrdersDao
) : OrdersRepository {

    override fun observeOrders(): Flow<List<Order>> =
        ordersDao.observeOrders().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createOrder(order: Order) {
        val createdOrder = order.copy(id = 0L, status = OrderStatus.AVAILABLE, acceptedByUserId = null, acceptedAtMillis = null)
        val orderId = ordersDao.insertOrder(createdOrder.toEntity())
        logDebug(
            action = "createOrder",
            orderId = orderId,
            oldStatus = null,
            newStatus = createdOrder.status
        )
    }

    override suspend fun acceptOrder(id: Long, acceptedByUserId: String, acceptedAtMillis: Long) {
        mutateOrder("acceptOrder", id) { order ->
            order.copy(
                status = OrderStatus.IN_PROGRESS,
                acceptedByUserId = acceptedByUserId,
                acceptedAtMillis = acceptedAtMillis
            )
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        mutateOrder("cancelOrder", id) { order ->
            order.copy(status = OrderStatus.CANCELED)
        }
    }

    override suspend fun completeOrder(id: Long) {
        mutateOrder("completeOrder", id) { order ->
            order.copy(status = OrderStatus.COMPLETED)
        }
    }

    override suspend fun getOrderById(id: Long): Order? =
        ordersDao.getOrderById(id)?.toDomain()

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val expirationThreshold = now - ORDER_EXPIRATION_GRACE_MS
        ordersDao.getOrders().forEach { entity ->
            val order = entity.toDomain()
            val shouldExpire = order.status == OrderStatus.AVAILABLE &&
                order.orderTime is OrderTime.Exact &&
                order.dateTime < expirationThreshold

            if (shouldExpire) {
                val expired = order.copy(status = OrderStatus.EXPIRED)
                ordersDao.updateOrder(expired.toEntity())
                logDebug(
                    action = "refresh",
                    orderId = order.id,
                    oldStatus = order.status,
                    newStatus = expired.status
                )
            }
        }
    }

    private suspend fun mutateOrder(action: String, id: Long, mutate: (Order) -> Order) {
        val current = ordersDao.getOrderById(id)?.toDomain() ?: return
        val updated = mutate(current)
        ordersDao.updateOrder(updated.toEntity())
        logDebug(
            action = action,
            orderId = id,
            oldStatus = current.status,
            newStatus = updated.status
        )
    }

    private fun logDebug(action: String, orderId: Long, oldStatus: OrderStatus?, newStatus: OrderStatus) {
        Log.d(LOG_TAG, "$action: id=$orderId, ${oldStatus ?: "NONE"}->$newStatus")
    }

    private companion object {
        const val LOG_TAG = "OrdersRepo"
        const val ORDER_EXPIRATION_GRACE_MS = 60_000L
    }
}
