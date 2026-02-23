package com.loaderapp.data.datasource.local

import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.OrderWorker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * LocalDataSource для работы с заказами через Room
 * Инкапсулирует работу с DAO
 */
class OrderLocalDataSource @Inject constructor(
    private val orderDao: OrderDao,
    private val orderWorkerDao: OrderWorkerDao
) {
    
    // Order operations
    
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()
    
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> = 
        orderDao.getOrdersByStatus(status)
    
    fun getOrdersByWorker(workerId: Long): Flow<List<Order>> = 
        orderDao.getOrdersByWorker(workerId)
    
    fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<Order>> = 
        orderDao.getOrdersByDispatcher(dispatcherId)
    
    suspend fun getOrderById(orderId: Long): Order? = 
        orderDao.getOrderById(orderId)

    fun getOrderByIdFlow(orderId: Long): Flow<Order?> =
        orderDao.getOrderByIdFlow(orderId)
    
    fun searchOrders(query: String, status: OrderStatus? = null): Flow<List<Order>> = 
        orderDao.searchOrders(query, status)
    
    fun searchOrdersByDispatcher(dispatcherId: Long, query: String): Flow<List<Order>> = 
        orderDao.searchOrdersByDispatcher(dispatcherId, query)
    
    suspend fun insertOrder(order: Order): Long = 
        orderDao.insertOrder(order)
    
    suspend fun updateOrder(order: Order) = 
        orderDao.updateOrder(order)
    
    suspend fun deleteOrder(order: Order) = 
        orderDao.deleteOrder(order)
    
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus) = 
        orderDao.updateOrderStatus(orderId, status)
    
    suspend fun completeOrder(orderId: Long, status: OrderStatus, completedAt: Long) = 
        orderDao.completeOrder(orderId, status, completedAt)
    
    suspend fun rateOrder(orderId: Long, rating: Float) = 
        orderDao.rateOrder(orderId, rating)
    
    // Statistics
    
    fun getCompletedOrdersCount(workerId: Long): Flow<Int> = 
        orderDao.getCompletedOrdersCount(workerId)
    
    fun getTotalEarnings(workerId: Long): Flow<Double?> = 
        orderDao.getTotalEarnings(workerId)
    
    fun getAverageRating(workerId: Long): Flow<Float?> = 
        orderDao.getAverageRating(workerId)
    
    fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> = 
        orderDao.getDispatcherCompletedCount(dispatcherId)
    
    fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> = 
        orderDao.getDispatcherActiveCount(dispatcherId)
    
    // OrderWorker operations
    
    suspend fun addWorkerToOrder(orderWorker: OrderWorker) = 
        orderWorkerDao.addWorkerToOrder(orderWorker)
    
    fun getWorkerCount(orderId: Long): Flow<Int> = 
        orderWorkerDao.getWorkerCount(orderId)
    
    suspend fun getWorkerCountSync(orderId: Long): Int = 
        orderWorkerDao.getWorkerCountSync(orderId)
    
    suspend fun hasWorker(orderId: Long, workerId: Long): Boolean = 
        orderWorkerDao.hasWorker(orderId, workerId)
    
    fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> = 
        orderWorkerDao.getOrderIdsByWorker(workerId)
}
