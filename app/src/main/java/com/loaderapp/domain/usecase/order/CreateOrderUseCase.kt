package com.loaderapp.domain.usecase.order

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Параметры для создания заказа
 */
data class CreateOrderParams(val order: OrderModel)

/**
 * UseCase: Создать новый заказ
 * 
 * Бизнес-правила:
 * - requiredWorkers >= 1
 * - estimatedHours >= 1
 * - pricePerHour > 0
 */
class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : UseCase<CreateOrderParams, Long>() {
    
    override suspend fun execute(params: CreateOrderParams): Result<Long> {
        val order = params.order
        
        // Валидация бизнес-правил
        if (order.requiredWorkers < 1) {
            return Result.Error("Требуется минимум 1 грузчик")
        }
        
        if (order.estimatedHours < 1) {
            return Result.Error("Минимальная длительность заказа - 1 час")
        }
        
        if (order.pricePerHour <= 0) {
            return Result.Error("Цена должна быть больше 0")
        }
        
        if (order.address.isBlank()) {
            return Result.Error("Адрес не может быть пустым")
        }
        
        if (order.cargoDescription.isBlank()) {
            return Result.Error("Описание груза не может быть пустым")
        }
        
        // Создание заказа
        return orderRepository.createOrder(order)
    }
}
