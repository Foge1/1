package com.loaderapp.data.datasource.local

import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.OrderWorker
import javax.inject.Inject

/**
 * LocalDataSource для работы с заказами через Room
 * Инкапсулирует работу с DAO
 */
class OrderLocalDataSource
    @Inject
    constructor(
        orderDao: OrderDao,
        orderWorkerDao: OrderWorkerDao,
    ) : OrderLocalDataSourceQueries(orderDao, orderWorkerDao) {
        suspend fun insertOrder(order: Order): Long = orderDao.insertOrder(order)

        suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)

        suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)

        suspend fun updateOrderStatus(
            orderId: Long,
            status: OrderStatus,
        ) = orderDao.updateOrderStatus(orderId, status)

        suspend fun completeOrder(
            orderId: Long,
            status: OrderStatus,
            completedAt: Long,
        ) = orderDao.completeOrder(orderId, status, completedAt)

        suspend fun rateOrder(
            orderId: Long,
            rating: Float,
        ) = orderDao.rateOrder(orderId, rating)

        suspend fun addWorkerToOrder(orderWorker: OrderWorker) = orderWorkerDao.addWorkerToOrder(orderWorker)
    }
