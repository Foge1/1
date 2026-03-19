package com.loaderapp.data.repository

import com.loaderapp.core.common.Result
import com.loaderapp.core.logging.AppLogger
import com.loaderapp.data.datasource.local.OrderLocalDataSource
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Реализация OrderRepository
 * Использует LocalDataSource и Mapper для работы с данными
 */
class OrderRepositoryImpl
    @Inject
    constructor(
        localDataSource: OrderLocalDataSource,
        appLogger: AppLogger,
    ) : OrderRepositoryQueries(localDataSource),
        OrderRepository {
        private val transitions =
            OrderRepositoryTransitions(
                localDataSource = localDataSource,
                appLogger = appLogger,
                logTag = LOG_TAG,
            )

        override suspend fun getOrderById(orderId: Long): Result<OrderModel> = transitions.getOrderById(orderId)

        override suspend fun createOrder(order: OrderModel): Result<Long> = transitions.createOrder(order)

        override suspend fun updateOrder(order: OrderModel): Result<Unit> = transitions.updateOrder(order)

        override suspend fun deleteOrder(order: OrderModel): Result<Unit> = transitions.deleteOrder(order)

        override suspend fun takeOrder(
            orderId: Long,
            workerId: Long,
        ): Result<Unit> = transitions.takeOrder(orderId = orderId, workerId = workerId)

        override suspend fun completeOrder(orderId: Long): Result<Unit> = transitions.completeOrder(orderId)

        override suspend fun cancelOrder(orderId: Long): Result<Unit> = transitions.cancelOrder(orderId)

        override suspend fun rateOrder(
            orderId: Long,
            rating: Float,
        ): Result<Unit> = transitions.rateOrder(orderId = orderId, rating = rating)

        companion object {
            private const val LOG_TAG = "OrderRepository"
        }
    }
