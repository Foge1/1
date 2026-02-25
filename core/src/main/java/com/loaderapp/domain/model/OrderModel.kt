package com.loaderapp.domain.model

/**
 * Domain модель заказа
 * Чистая бизнес-модель без зависимостей от фреймворка
 */
data class OrderModel(
    val id: Long,
    val address: String,
    val dateTime: Long,
    val cargoDescription: String,
    val pricePerHour: Double,
    val estimatedHours: Int,
    val requiredWorkers: Int,
    val minWorkerRating: Float,
    val status: OrderStatusModel,
    val createdAt: Long,
    val completedAt: Long?,
    val workerId: Long?,
    val dispatcherId: Long,
    val workerRating: Float?,
    val comment: String,
    val isAsap: Boolean = false
) {
    /**
     * Вычисляемое свойство: общая стоимость заказа
     */
    val totalPrice: Double
        get() = pricePerHour * estimatedHours
    
    /**
     * Проверка, доступен ли заказ для взятия
     */
    fun isAvailableForWorker(workerRating: Float): Boolean {
        return status == OrderStatusModel.AVAILABLE && workerRating >= minWorkerRating
    }
    
    /**
     * Проверка, можно ли завершить заказ
     */
    fun canBeCompleted(): Boolean {
        return status == OrderStatusModel.TAKEN || status == OrderStatusModel.IN_PROGRESS
    }
    
    /**
     * Проверка, можно ли отменить заказ
     */
    fun canBeCancelled(): Boolean {
        return status != OrderStatusModel.COMPLETED && status != OrderStatusModel.CANCELLED
    }
}

enum class OrderStatusModel {
    AVAILABLE,
    TAKEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;
    
    /**
     * Получить человеко-читаемое название статуса
     */
    fun getDisplayName(): String = when (this) {
        AVAILABLE -> "Доступен"
        TAKEN -> "Взят"
        IN_PROGRESS -> "В работе"
        COMPLETED -> "Завершён"
        CANCELLED -> "Отменён"
    }
}
