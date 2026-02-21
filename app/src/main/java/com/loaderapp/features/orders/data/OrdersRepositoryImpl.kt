package com.loaderapp.features.orders.data

import android.util.Log
import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStateMachine
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTransitionResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class OrdersRepositoryImpl @Inject constructor() : OrdersRepository {
    private val ordersFlow = MutableStateFlow<List<Order>>(emptyList())

    override fun observeOrders(): Flow<List<Order>> = ordersFlow.asStateFlow()

    override suspend fun createOrder(order: Order) {
        val nextId = (ordersFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val createdOrder = order.copy(id = nextId, status = OrderStatus.AVAILABLE)
        ordersFlow.update { current -> current + createdOrder }
        logDebug(
            action = "createOrder",
            orderId = createdOrder.id,
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
        ordersFlow.update { current ->
            current.map { order ->
                if (order.status == OrderStatus.AVAILABLE && order.dateTime < now) {
                    when (val result = OrderStateMachine.transition(order, OrderStatus.EXPIRED)) {
                        is OrderTransitionResult.Success -> {
                            logDebug(
                                action = "refresh",
                                orderId = order.id,
                                oldStatus = order.status,
                                newStatus = result.order.status
                            )
                            result.order
                        }

                        is OrderTransitionResult.Failure -> order
                    }
                } else {
                    order
                }
            }
        }
    }

    private fun mutateOrder(action: String, id: Long, mutate: (Order) -> Order) {
        var oldStatus: OrderStatus? = null
        var updatedStatus: OrderStatus? = null
        ordersFlow.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                return@update current
            }
            current.mapIndexed { orderIndex, order ->
                if (orderIndex == index) {
                    oldStatus = order.status
                    val updated = mutate(order)
                    updatedStatus = updated.status
                    updated
                } else {
                    order
                }
            }
        }
        if (oldStatus != null && updatedStatus != null) {
            logDebug(
                action = action,
                orderId = id,
                oldStatus = oldStatus,
                newStatus = updatedStatus
            )
        }
    }

    private fun logDebug(action: String, orderId: Long, oldStatus: OrderStatus?, newStatus: OrderStatus) {
        Log.d(LOG_TAG, "$action: id=$orderId, ${oldStatus ?: "NONE"}->$newStatus")
    }

    private companion object {
        const val LOG_TAG = "OrdersRepo"
    }
}
