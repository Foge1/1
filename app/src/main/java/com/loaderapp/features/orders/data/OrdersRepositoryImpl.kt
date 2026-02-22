package com.loaderapp.features.orders.data

import android.util.Log
import com.loaderapp.features.orders.data.local.dao.OrdersDao
import com.loaderapp.features.orders.data.mappers.toDomain
import com.loaderapp.features.orders.data.mappers.toEntity
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.OrderTransitionResult
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
        val createdOrder = order.copy(id = 0L, status = OrderStatus.AVAILABLE)
        val orderId = ordersDao.insertOrder(createdOrder.toEntity())
        logDebug(
            action = "createOrder",
            orderId = orderId,
            oldStatus = null,
            newStatus = createdOrder.status
        )
    }

    override suspend fun acceptOrder(id: Long) {
        mutateOrder("acceptOrder", id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.IN_PROGRESS)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun cancelOrder(id: Long, reason: String?) {
        mutateOrder("cancelOrder", id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.CANCELED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun completeOrder(id: Long) {
        mutateOrder("completeOrder", id) { order ->
            when (val result = OrderStateMachine.transition(order, OrderStatus.COMPLETED)) {
                is OrderTransitionResult.Success -> result.order
                is OrderTransitionResult.Failure -> order
            }
        }
    }

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val expirationThreshold = now - ORDER_EXPIRATION_GRACE_MS
        ordersDao.getOrders().forEach { entity ->
            val order = entity.toDomain()
            val shouldExpire = order.status == OrderStatus.AVAILABLE &&
                order.orderTime is OrderTime.Exact &&
                order.dateTime < expirationThreshold

            if (shouldExpire) {
                when (val result = OrderStateMachine.transition(order, OrderStatus.EXPIRED)) {
                    is OrderTransitionResult.Success -> {
                        ordersDao.updateOrder(result.order.toEntity())
                        logDebug(
                            action = "refresh",
                            orderId = order.id,
                            oldStatus = order.status,
                            newStatus = result.order.status
                        )
                    }

                    is OrderTransitionResult.Failure -> Unit
                }
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
