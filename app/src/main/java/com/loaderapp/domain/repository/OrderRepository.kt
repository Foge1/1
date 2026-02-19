package com.loaderapp.domain.repository

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с заказами
 * Определяет контракт для data слоя, независим от реализации
 */
interface OrderRepository {
    
    /**
     * Получить все заказы
     */
    fun getAllOrders(): Flow<List<OrderModel>>
    
    /**
     * Получить доступные заказы
     */
    fun getAvailableOrders(): Flow<List<OrderModel>>
    
    /**
     * Получить заказы по грузчику
     */
    fun getOrdersByWorker(workerId: Long): Flow<List<OrderModel>>
    
    /**
     * Получить заказы по диспетчеру
     */
    fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<OrderModel>>
    
    /**
     * Получить заказ по ID
     */
    suspend fun getOrderById(orderId: Long): Result<OrderModel>
    
    /**
     * Поиск заказов
     */
    fun searchOrders(query: String, status: OrderStatusModel? = null): Flow<List<OrderModel>>
    
    /**
     * Поиск заказов диспетчера
     */
    fun searchOrdersByDispatcher(dispatcherId: Long, query: String): Flow<List<OrderModel>>
    
    /**
     * Создать заказ
     */
    suspend fun createOrder(order: OrderModel): Result<Long>
    
    /**
     * Обновить заказ
     */
    suspend fun updateOrder(order: OrderModel): Result<Unit>
    
    /**
     * Удалить заказ
     */
    suspend fun deleteOrder(order: OrderModel): Result<Unit>
    
    /**
     * Взять заказ грузчиком
     */
    suspend fun takeOrder(orderId: Long, workerId: Long): Result<Unit>
    
    /**
     * Завершить заказ
     */
    suspend fun completeOrder(orderId: Long): Result<Unit>
    
    /**
     * Отменить заказ
     */
    suspend fun cancelOrder(orderId: Long): Result<Unit>
    
    /**
     * Оценить заказ
     */
    suspend fun rateOrder(orderId: Long, rating: Float): Result<Unit>
    
    /**
     * Получить количество грузчиков на заказе
     */
    fun getWorkerCountForOrder(orderId: Long): Flow<Int>
    
    /**
     * Получить количество грузчиков синхронно
     */
    suspend fun getWorkerCountSync(orderId: Long): Int
    
    /**
     * Проверить, взял ли грузчик заказ
     */
    suspend fun hasWorkerTakenOrder(orderId: Long, workerId: Long): Boolean
    
    /**
     * Получить ID заказов по грузчику
     */
    fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>>
    
    // Статистика
    
    /**
     * Получить количество завершённых заказов грузчика
     */
    fun getCompletedOrdersCount(workerId: Long): Flow<Int>
    
    /**
     * Получить общий заработок грузчика
     */
    fun getTotalEarnings(workerId: Long): Flow<Double?>
    
    /**
     * Получить средний рейтинг грузчика
     */
    fun getAverageRating(workerId: Long): Flow<Float?>
    
    /**
     * Получить количество завершённых заказов диспетчера
     */
    fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int>
    
    /**
     * Получить количество активных заказов диспетчера
     */
    fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int>
}
