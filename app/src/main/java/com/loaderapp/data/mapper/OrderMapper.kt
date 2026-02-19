package com.loaderapp.data.mapper

import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel

/**
 * Mapper для конвертации Order между data и domain слоями
 */
object OrderMapper {
    
    /**
     * Конвертация Data Entity -> Domain Model
     */
    fun toDomain(entity: Order): OrderModel {
        return OrderModel(
            id = entity.id,
            address = entity.address,
            dateTime = entity.dateTime,
            cargoDescription = entity.cargoDescription,
            pricePerHour = entity.pricePerHour,
            estimatedHours = entity.estimatedHours,
            requiredWorkers = entity.requiredWorkers,
            minWorkerRating = entity.minWorkerRating,
            status = entity.status.toDomain(),
            createdAt = entity.createdAt,
            completedAt = entity.completedAt,
            workerId = entity.workerId,
            dispatcherId = entity.dispatcherId,
            workerRating = entity.workerRating,
            comment = entity.comment
        )
    }
    
    /**
     * Конвертация Domain Model -> Data Entity
     */
    fun toEntity(model: OrderModel): Order {
        return Order(
            id = model.id,
            address = model.address,
            dateTime = model.dateTime,
            cargoDescription = model.cargoDescription,
            pricePerHour = model.pricePerHour,
            estimatedHours = model.estimatedHours,
            requiredWorkers = model.requiredWorkers,
            minWorkerRating = model.minWorkerRating,
            status = model.status.toEntity(),
            createdAt = model.createdAt,
            completedAt = model.completedAt,
            workerId = model.workerId,
            dispatcherId = model.dispatcherId,
            workerRating = model.workerRating,
            comment = model.comment
        )
    }
    
    /**
     * Конвертация списка Entity -> Domain
     */
    fun toDomainList(entities: List<Order>): List<OrderModel> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Конвертация списка Domain -> Entity
     */
    fun toEntityList(models: List<OrderModel>): List<Order> {
        return models.map { toEntity(it) }
    }
}

/**
 * Extension функции для OrderStatus
 */
private fun OrderStatus.toDomain(): OrderStatusModel {
    return when (this) {
        OrderStatus.AVAILABLE -> OrderStatusModel.AVAILABLE
        OrderStatus.TAKEN -> OrderStatusModel.TAKEN
        OrderStatus.IN_PROGRESS -> OrderStatusModel.IN_PROGRESS
        OrderStatus.COMPLETED -> OrderStatusModel.COMPLETED
        OrderStatus.CANCELLED -> OrderStatusModel.CANCELLED
    }
}

private fun OrderStatusModel.toEntity(): OrderStatus {
    return when (this) {
        OrderStatusModel.AVAILABLE -> OrderStatus.AVAILABLE
        OrderStatusModel.TAKEN -> OrderStatus.TAKEN
        OrderStatusModel.IN_PROGRESS -> OrderStatus.IN_PROGRESS
        OrderStatusModel.COMPLETED -> OrderStatus.COMPLETED
        OrderStatusModel.CANCELLED -> OrderStatus.CANCELLED
    }
}

