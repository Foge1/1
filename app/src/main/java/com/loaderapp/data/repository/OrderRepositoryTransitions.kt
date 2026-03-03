package com.loaderapp.data.repository

import com.loaderapp.core.common.Result
import com.loaderapp.core.logging.AppLogger
import com.loaderapp.data.datasource.local.OrderLocalDataSource
import com.loaderapp.data.mapper.OrderMapper
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.OrderWorker
import com.loaderapp.domain.model.OrderModel

internal class OrderRepositoryTransitions(
    private val localDataSource: OrderLocalDataSource,
    private val appLogger: AppLogger,
    private val logTag: String,
) {
    suspend fun getOrderById(orderId: Long): Result<OrderModel> =
        runCatching {
            val order = localDataSource.getOrderById(orderId)
            if (order != null) {
                Result.Success(OrderMapper.toDomain(order))
            } else {
                Result.Error("Заказ не найден")
            }
        }.getOrElse { error ->
            appLogger.breadcrumb("storage", "order_get_failed", mapOf("operation" to "get_by_id"))
            appLogger.e(logTag, "DB error while getting order", error)
            Result.Error("Ошибка получения заказа: ${error.message}", error)
        }

    suspend fun createOrder(order: OrderModel): Result<Long> =
        runCatching {
            val entity = OrderMapper.toEntity(order)
            val id = localDataSource.insertOrder(entity)
            appLogger.breadcrumb("orders", "order_created", mapOf("operation" to "create"))
            Result.Success(id)
        }.getOrElse { error ->
            appLogger.breadcrumb("storage", "order_create_failed", mapOf("operation" to "create"))
            appLogger.e(logTag, "DB error while creating order", error)
            Result.Error("Ошибка создания заказа: ${error.message}", error)
        }

    suspend fun updateOrder(order: OrderModel): Result<Unit> =
        runCatching {
            val entity = OrderMapper.toEntity(order)
            localDataSource.updateOrder(entity)
            appLogger.breadcrumb("orders", "order_updated", mapOf("operation" to "update"))
            Result.Success(Unit)
        }.getOrElse { error ->
            appLogger.breadcrumb("storage", "order_update_failed", mapOf("operation" to "update"))
            appLogger.e(logTag, "DB error while updating order", error)
            Result.Error("Ошибка обновления заказа: ${error.message}", error)
        }

    suspend fun deleteOrder(order: OrderModel): Result<Unit> =
        runCatching {
            localDataSource.deleteOrder(OrderMapper.toEntity(order))
            Result.Success(Unit)
        }.getOrElse { error ->
            Result.Error("Ошибка удаления заказа: ${error.message}", error)
        }

    suspend fun takeOrder(
        orderId: Long,
        workerId: Long,
    ): Result<Unit> =
        runCatching {
            localDataSource.addWorkerToOrder(OrderWorker(orderId = orderId, workerId = workerId))
            updateTakenStatusIfNeeded(orderId = orderId, workerId = workerId)
            Result.Success(Unit)
        }.getOrElse { error ->
            Result.Error("Ошибка взятия заказа: ${error.message}", error)
        }

    suspend fun completeOrder(orderId: Long): Result<Unit> =
        runCatching {
            localDataSource.completeOrder(orderId, OrderStatus.COMPLETED, System.currentTimeMillis())
            Result.Success(Unit)
        }.getOrElse { error ->
            Result.Error("Ошибка завершения заказа: ${error.message}", error)
        }

    suspend fun cancelOrder(orderId: Long): Result<Unit> =
        runCatching {
            localDataSource.updateOrderStatus(orderId, OrderStatus.CANCELLED)
            Result.Success(Unit)
        }.getOrElse { error ->
            Result.Error("Ошибка отмены заказа: ${error.message}", error)
        }

    suspend fun rateOrder(
        orderId: Long,
        rating: Float,
    ): Result<Unit> =
        runCatching {
            localDataSource.rateOrder(orderId, rating)
            Result.Success(Unit)
        }.getOrElse { error ->
            Result.Error("Ошибка оценки заказа: ${error.message}", error)
        }

    private suspend fun updateTakenStatusIfNeeded(
        orderId: Long,
        workerId: Long,
    ) {
        val order = localDataSource.getOrderById(orderId) ?: return
        if (order.workerId == null) {
            localDataSource.updateOrder(order.copy(workerId = workerId, status = OrderStatus.TAKEN))
            return
        }

        val count = localDataSource.getWorkerCountSync(orderId)
        if (count >= order.requiredWorkers) {
            localDataSource.updateOrderStatus(orderId, OrderStatus.TAKEN)
        }
    }
}
