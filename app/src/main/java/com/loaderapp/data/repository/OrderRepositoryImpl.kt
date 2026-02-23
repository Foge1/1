package com.loaderapp.data.repository

import com.loaderapp.core.common.Result
import com.loaderapp.data.datasource.local.OrderLocalDataSource
import com.loaderapp.data.mapper.OrderMapper
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.OrderWorker
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Реализация OrderRepository
 * Использует LocalDataSource и Mapper для работы с данными
 */
class OrderRepositoryImpl @Inject constructor(
    private val localDataSource: OrderLocalDataSource
) : OrderRepository {
    
    override fun getAllOrders(): Flow<List<OrderModel>> {
        return localDataSource.getAllOrders()
            .map { OrderMapper.toDomainList(it) }
    }
    
    override fun getAvailableOrders(): Flow<List<OrderModel>> {
        return localDataSource.getOrdersByStatus(OrderStatus.AVAILABLE)
            .map { OrderMapper.toDomainList(it) }
    }
    
    override fun getOrdersByWorker(workerId: Long): Flow<List<OrderModel>> {
        return combine(
            localDataSource.getAllOrders(),
            localDataSource.getOrderIdsByWorker(workerId)
        ) { orders, orderIds ->
            val relatedIds = orderIds.toSet()
            orders.filter { order ->
                order.workerId == workerId || order.id in relatedIds
            }
        }.map { OrderMapper.toDomainList(it) }
    }
    
    override fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<OrderModel>> {
        return localDataSource.getOrdersByDispatcher(dispatcherId)
            .map { OrderMapper.toDomainList(it) }
    }
    

    override fun getOrderByIdFlow(orderId: Long): Flow<OrderModel?> {
        return localDataSource.getOrderByIdFlow(orderId)
            .map { it?.let(OrderMapper::toDomain) }
    }

    override suspend fun getOrderById(orderId: Long): Result<OrderModel> {
        return try {
            val order = localDataSource.getOrderById(orderId)
            if (order != null) {
                Result.Success(OrderMapper.toDomain(order))
            } else {
                Result.Error("Заказ не найден")
            }
        } catch (e: Exception) {
            Result.Error("Ошибка получения заказа: ${e.message}", e)
        }
    }
    
    override fun searchOrders(query: String, status: OrderStatusModel?): Flow<List<OrderModel>> {
        val entityStatus = status?.let { 
            when (it) {
                OrderStatusModel.AVAILABLE -> OrderStatus.AVAILABLE
                OrderStatusModel.TAKEN -> OrderStatus.TAKEN
                OrderStatusModel.IN_PROGRESS -> OrderStatus.IN_PROGRESS
                OrderStatusModel.COMPLETED -> OrderStatus.COMPLETED
                OrderStatusModel.CANCELLED -> OrderStatus.CANCELLED
            }
        }
        return localDataSource.searchOrders(query, entityStatus)
            .map { OrderMapper.toDomainList(it) }
    }
    
    override fun searchOrdersByDispatcher(dispatcherId: Long, query: String): Flow<List<OrderModel>> {
        return localDataSource.searchOrdersByDispatcher(dispatcherId, query)
            .map { OrderMapper.toDomainList(it) }
    }
    
    override suspend fun createOrder(order: OrderModel): Result<Long> {
        return try {
            val entity = OrderMapper.toEntity(order)
            val id = localDataSource.insertOrder(entity)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error("Ошибка создания заказа: ${e.message}", e)
        }
    }
    
    override suspend fun updateOrder(order: OrderModel): Result<Unit> {
        return try {
            val entity = OrderMapper.toEntity(order)
            localDataSource.updateOrder(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Ошибка обновления заказа: ${e.message}", e)
        }
    }
    
    override suspend fun deleteOrder(order: OrderModel): Result<Unit> {
        return try {
            val entity = OrderMapper.toEntity(order)
            localDataSource.deleteOrder(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Ошибка удаления заказа: ${e.message}", e)
        }
    }
    
    override suspend fun takeOrder(orderId: Long, workerId: Long): Result<Unit> {
        return try {
            // Добавляем грузчика в таблицу order_workers
            localDataSource.addWorkerToOrder(OrderWorker(orderId = orderId, workerId = workerId))
            
            // Обновляем workerId в заказе (первый взявший)
            val order = localDataSource.getOrderById(orderId)
            if (order != null && order.workerId == null) {
                localDataSource.updateOrder(order.copy(workerId = workerId, status = OrderStatus.TAKEN))
            } else if (order != null) {
                // Уже есть грузчики — проверяем набрали ли нужное количество
                val count = localDataSource.getWorkerCountSync(orderId)
                if (count >= order.requiredWorkers) {
                    localDataSource.updateOrderStatus(orderId, OrderStatus.TAKEN)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Ошибка взятия заказа: ${e.message}", e)
        }
    }
    
    override suspend fun completeOrder(orderId: Long): Result<Unit> {
        return try {
            localDataSource.completeOrder(orderId, OrderStatus.COMPLETED, System.currentTimeMillis())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Ошибка завершения заказа: ${e.message}", e)
        }
    }
    
    override suspend fun cancelOrder(orderId: Long): Result<Unit> {
        return try {
            localDataSource.updateOrderStatus(orderId, OrderStatus.CANCELLED)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Ошибка отмены заказа: ${e.message}", e)
        }
    }
    
    override suspend fun rateOrder(orderId: Long, rating: Float): Result<Unit> {
        return try {
            localDataSource.rateOrder(orderId, rating)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Ошибка оценки заказа: ${e.message}", e)
        }
    }
    
    override fun getWorkerCountForOrder(orderId: Long): Flow<Int> {
        return localDataSource.getWorkerCount(orderId)
    }
    
    override suspend fun getWorkerCountSync(orderId: Long): Int {
        return localDataSource.getWorkerCountSync(orderId)
    }
    
    override suspend fun hasWorkerTakenOrder(orderId: Long, workerId: Long): Boolean {
        return localDataSource.hasWorker(orderId, workerId)
    }
    
    override fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> {
        return localDataSource.getOrderIdsByWorker(workerId)
    }
    
    // Statistics
    
    override fun getCompletedOrdersCount(workerId: Long): Flow<Int> {
        return localDataSource.getCompletedOrdersCount(workerId)
    }
    
    override fun getTotalEarnings(workerId: Long): Flow<Double?> {
        return localDataSource.getTotalEarnings(workerId)
    }
    
    override fun getAverageRating(workerId: Long): Flow<Float?> {
        return localDataSource.getAverageRating(workerId)
    }
    
    override fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> {
        return localDataSource.getDispatcherCompletedCount(dispatcherId)
    }
    
    override fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> {
        return localDataSource.getDispatcherActiveCount(dispatcherId)
    }
}
