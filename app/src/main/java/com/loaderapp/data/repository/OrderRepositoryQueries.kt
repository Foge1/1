package com.loaderapp.data.repository

import com.loaderapp.data.datasource.local.OrderLocalDataSource
import com.loaderapp.data.mapper.OrderMapper
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

abstract class OrderRepositoryQueries(
    protected val localDataSource: OrderLocalDataSource,
) : OrderRepository {
    final override fun getAllOrders(): Flow<List<OrderModel>> =
        localDataSource
            .getAllOrders()
            .map(OrderMapper::toDomainList)

    final override fun getAvailableOrders(): Flow<List<OrderModel>> =
        localDataSource
            .getOrdersByStatus(OrderStatus.AVAILABLE)
            .map(OrderMapper::toDomainList)

    final override fun getOrdersByWorker(workerId: Long): Flow<List<OrderModel>> =
        combine(
            localDataSource.getAllOrders(),
            localDataSource.getOrderIdsByWorker(workerId),
        ) { orders, orderIds ->
            val relatedIds = orderIds.toSet()
            orders.filter { order ->
                order.workerId == workerId || order.id in relatedIds
            }
        }.map(OrderMapper::toDomainList)

    final override fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<OrderModel>> =
        localDataSource
            .getOrdersByDispatcher(dispatcherId)
            .map(OrderMapper::toDomainList)

    final override fun getOrderByIdFlow(orderId: Long): Flow<OrderModel?> =
        localDataSource
            .getOrderByIdFlow(orderId)
            .map { order -> order?.let(OrderMapper::toDomain) }

    final override fun searchOrders(
        query: String,
        status: OrderStatusModel?,
    ): Flow<List<OrderModel>> =
        localDataSource
            .searchOrders(query = query, status = status?.toEntity())
            .map(OrderMapper::toDomainList)

    final override fun searchOrdersByDispatcher(
        dispatcherId: Long,
        query: String,
    ): Flow<List<OrderModel>> =
        localDataSource
            .searchOrdersByDispatcher(dispatcherId = dispatcherId, query = query)
            .map(OrderMapper::toDomainList)

    final override fun getWorkerCountForOrder(orderId: Long): Flow<Int> = localDataSource.getWorkerCount(orderId)

    final override suspend fun getWorkerCountSync(orderId: Long): Int = localDataSource.getWorkerCountSync(orderId)

    final override suspend fun hasWorkerTakenOrder(
        orderId: Long,
        workerId: Long,
    ): Boolean = localDataSource.hasWorker(orderId, workerId)

    final override fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> = localDataSource.getOrderIdsByWorker(workerId)

    final override fun getCompletedOrdersCount(workerId: Long): Flow<Int> = localDataSource.getCompletedOrdersCount(workerId)

    final override fun getTotalEarnings(workerId: Long): Flow<Double?> = localDataSource.getTotalEarnings(workerId)

    final override fun getAverageRating(workerId: Long): Flow<Float?> = localDataSource.getAverageRating(workerId)

    final override fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> =
        localDataSource.getDispatcherCompletedCount(dispatcherId)

    final override fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> = localDataSource.getDispatcherActiveCount(dispatcherId)
}

internal fun OrderStatusModel.toEntity(): OrderStatus =
    when (this) {
        OrderStatusModel.AVAILABLE -> OrderStatus.AVAILABLE
        OrderStatusModel.TAKEN -> OrderStatus.TAKEN
        OrderStatusModel.IN_PROGRESS -> OrderStatus.IN_PROGRESS
        OrderStatusModel.COMPLETED -> OrderStatus.COMPLETED
        OrderStatusModel.CANCELLED -> OrderStatus.CANCELLED
    }
