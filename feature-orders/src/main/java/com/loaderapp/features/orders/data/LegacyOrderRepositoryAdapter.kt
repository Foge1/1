package com.loaderapp.features.orders.data

import com.loaderapp.core.common.Result
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.features.orders.data.mappers.toFeatureOrderModel
import com.loaderapp.features.orders.data.mappers.toFeatureStatus
import com.loaderapp.features.orders.data.mappers.toLegacyOrderModel
import com.loaderapp.features.orders.domain.OrderAssignmentStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.loaderapp.domain.model.OrderModel as LegacyOrderModel
import com.loaderapp.domain.model.OrderStatusModel as LegacyOrderStatusModel

@Singleton
class LegacyOrderRepositoryAdapter
    @Inject
    constructor(
        private val ordersRepository: OrdersRepository,
    ) : OrderRepository {
        override fun getAllOrders(): Flow<List<LegacyOrderModel>> =
            ordersRepository.observeOrders().map { orders -> orders.map { it.toLegacyOrderModel() } }

        override fun getAvailableOrders(): Flow<List<LegacyOrderModel>> =
            ordersRepository
                .observeOrders()
                .map { orders -> orders.filter { it.status == OrderStatus.STAFFING }.map { it.toLegacyOrderModel() } }

        override fun getOrdersByWorker(workerId: Long): Flow<List<LegacyOrderModel>> {
            val workerKey = workerId.toString()
            return ordersRepository
                .observeOrders()
                .map { orders ->
                    orders
                        .filter { order ->
                            order.assignments.any { it.loaderId == workerKey } ||
                                order.applications.any { it.loaderId == workerKey }
                        }.map { it.toLegacyOrderModel() }
                }
        }

        override fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<LegacyOrderModel>> =
            ordersRepository
                .observeOrders()
                .map { orders ->
                    orders
                        .filter { it.meta[DISPATCHER_ID_KEY]?.toLongOrNull() == dispatcherId }
                        .map { it.toLegacyOrderModel() }
                }

        override suspend fun getOrderById(orderId: Long): Result<LegacyOrderModel> =
            ordersRepository.getOrderById(orderId)?.let { Result.Success(it.toLegacyOrderModel()) }
                ?: Result.Error("Order not found")

        override fun getOrderByIdFlow(orderId: Long): Flow<LegacyOrderModel?> =
            ordersRepository.observeOrders().map { orders -> orders.firstOrNull { it.id == orderId }?.toLegacyOrderModel() }

        override fun searchOrders(
            query: String,
            status: LegacyOrderStatusModel?,
        ): Flow<List<LegacyOrderModel>> =
            ordersRepository
                .observeOrders()
                .map { orders ->
                    orders
                        .filter { order ->
                            val byQuery =
                                query.isBlank() ||
                                    order.title.contains(query, ignoreCase = true) ||
                                    order.address.contains(query, ignoreCase = true) ||
                                    order.comment.orEmpty().contains(query, ignoreCase = true)
                            val byStatus = status == null || order.status == status.toFeatureStatus()
                            byQuery && byStatus
                        }.map { it.toLegacyOrderModel() }
                }

        override fun searchOrdersByDispatcher(
            dispatcherId: Long,
            query: String,
        ): Flow<List<LegacyOrderModel>> =
            getOrdersByDispatcher(dispatcherId).map { orders ->
                if (query.isBlank()) {
                    orders
                } else {
                    orders.filter { order ->
                        order.cargoDescription.contains(query, ignoreCase = true) ||
                            order.address.contains(query, ignoreCase = true) ||
                            order.comment.contains(query, ignoreCase = true)
                    }
                }
            }

        override suspend fun createOrder(order: LegacyOrderModel): Result<Long> =
            runCatching {
                val createdOrderId = ordersRepository.createOrder(order.toFeatureOrderModel())
                Result.Success(createdOrderId)
            }.getOrElse { Result.Error(it.message ?: "Failed to create order") }

        override suspend fun updateOrder(order: LegacyOrderModel): Result<Unit> = Result.Error("Not supported")

        override suspend fun deleteOrder(order: LegacyOrderModel): Result<Unit> =
            runCatching {
                ordersRepository.cancelOrder(order.id, reason = "legacy_delete")
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "Failed to delete order") }

        override suspend fun takeOrder(
            orderId: Long,
            workerId: Long,
        ): Result<Unit> =
            runCatching {
                ordersRepository.applyToOrder(orderId, workerId.toString(), now = System.currentTimeMillis())
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "Failed to take order") }

        override suspend fun completeOrder(orderId: Long): Result<Unit> =
            runCatching {
                ordersRepository.completeOrder(orderId)
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "Failed to complete order") }

        override suspend fun cancelOrder(orderId: Long): Result<Unit> =
            runCatching {
                ordersRepository.cancelOrder(orderId)
                Result.Success(Unit)
            }.getOrElse { Result.Error(it.message ?: "Failed to cancel order") }

        override suspend fun rateOrder(
            orderId: Long,
            rating: Float,
        ): Result<Unit> = Result.Error("Not supported")

        override fun getWorkerCountForOrder(orderId: Long): Flow<Int> = getOrderByIdFlow(orderId).map { it?.requiredWorkers ?: 0 }

        override suspend fun getWorkerCountSync(orderId: Long): Int = getWorkerCountForOrder(orderId).first()

        override suspend fun hasWorkerTakenOrder(
            orderId: Long,
            workerId: Long,
        ): Boolean =
            ordersRepository
                .getOrderById(orderId)
                ?.assignments
                ?.any { it.loaderId == workerId.toString() && it.status == OrderAssignmentStatus.ACTIVE } == true

        override fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> =
            ordersRepository.observeOrders().map { orders ->
                orders
                    .filter { order -> order.assignments.any { it.loaderId == workerId.toString() } }
                    .map { it.id }
            }

        override fun getCompletedOrdersCount(workerId: Long): Flow<Int> =
            getOrdersByWorker(workerId).map { orders -> orders.count { it.status == LegacyOrderStatusModel.COMPLETED } }

        override fun getTotalEarnings(workerId: Long): Flow<Double?> =
            getOrdersByWorker(workerId).map { orders ->
                orders
                    .filter { it.status == LegacyOrderStatusModel.COMPLETED }
                    .sumOf { it.pricePerHour * it.estimatedHours }
            }

        override fun getAverageRating(workerId: Long): Flow<Float?> =
            getOrdersByWorker(workerId).map { orders ->
                val ratings = orders.mapNotNull { it.workerRating }
                if (ratings.isEmpty()) null else ratings.average().toFloat()
            }

        override fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> =
            getOrdersByDispatcher(dispatcherId).map { orders -> orders.count { it.status == LegacyOrderStatusModel.COMPLETED } }

        override fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> =
            getOrdersByDispatcher(dispatcherId).map { orders ->
                orders.count { it.status == LegacyOrderStatusModel.AVAILABLE || it.status == LegacyOrderStatusModel.IN_PROGRESS }
            }
    }

private const val DISPATCHER_ID_KEY = "dispatcherId"
