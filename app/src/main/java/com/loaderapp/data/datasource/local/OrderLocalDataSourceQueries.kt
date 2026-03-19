package com.loaderapp.data.datasource.local

import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

abstract class OrderLocalDataSourceQueries(
    protected val orderDao: OrderDao,
    protected val orderWorkerDao: OrderWorkerDao,
) {
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> = orderDao.getOrdersByStatus(status)

    fun getOrdersByWorker(workerId: Long): Flow<List<Order>> = orderDao.getOrdersByWorker(workerId)

    fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<Order>> = orderDao.getOrdersByDispatcher(dispatcherId)

    suspend fun getOrderById(orderId: Long): Order? = orderDao.getOrderById(orderId)

    fun getOrderByIdFlow(orderId: Long): Flow<Order?> = orderDao.getOrderByIdFlow(orderId)

    fun searchOrders(
        query: String,
        status: OrderStatus? = null,
    ): Flow<List<Order>> = orderDao.searchOrders(query, status)

    fun searchOrdersByDispatcher(
        dispatcherId: Long,
        query: String,
    ): Flow<List<Order>> = orderDao.searchOrdersByDispatcher(dispatcherId, query)

    fun getCompletedOrdersCount(workerId: Long): Flow<Int> = orderDao.getCompletedOrdersCount(workerId)

    fun getTotalEarnings(workerId: Long): Flow<Double?> = orderDao.getTotalEarnings(workerId)

    fun getAverageRating(workerId: Long): Flow<Float?> = orderDao.getAverageRating(workerId)

    fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> = orderDao.getDispatcherCompletedCount(dispatcherId)

    fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> = orderDao.getDispatcherActiveCount(dispatcherId)

    fun getWorkerCount(orderId: Long): Flow<Int> = orderWorkerDao.getWorkerCount(orderId)

    suspend fun getWorkerCountSync(orderId: Long): Int = orderWorkerDao.getWorkerCountSync(orderId)

    suspend fun hasWorker(
        orderId: Long,
        workerId: Long,
    ): Boolean = orderWorkerDao.hasWorker(orderId, workerId)

    fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> = orderWorkerDao.getOrderIdsByWorker(workerId)
}
