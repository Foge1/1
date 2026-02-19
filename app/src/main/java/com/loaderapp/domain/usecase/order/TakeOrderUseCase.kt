package com.loaderapp.domain.usecase.order

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Параметры для взятия заказа
 */
data class TakeOrderParams(
    val orderId: Long,
    val workerId: Long
)

/**
 * UseCase: Грузчик берёт заказ
 * 
 * Бизнес-правила:
 * - Заказ должен быть в статусе AVAILABLE
 * - Рейтинг грузчика >= минимального рейтинга заказа
 * - Грузчик ещё не взял этот заказ
 * - Количество грузчиков < requiredWorkers
 */
class TakeOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) : UseCase<TakeOrderParams, Unit>() {
    
    override suspend fun execute(params: TakeOrderParams): Result<Unit> {
        // Получаем заказ
        val orderResult = orderRepository.getOrderById(params.orderId)
        if (orderResult is Result.Error) {
            return Result.Error("Заказ не найден")
        }
        val order = orderResult.getOrNull() ?: return Result.Error("Заказ не найден")
        
        // Проверка статуса заказа
        if (order.status != OrderStatusModel.AVAILABLE) {
            return Result.Error("Заказ уже взят или завершён")
        }
        
        // Получаем грузчика
        val workerResult = userRepository.getUserById(params.workerId)
        if (workerResult is Result.Error) {
            return Result.Error("Грузчик не найден")
        }
        val worker = workerResult.getOrNull() ?: return Result.Error("Грузчик не найден")
        
        // Проверка рейтинга
        if (worker.rating.toFloat() < order.minWorkerRating) {
            return Result.Error("Ваш рейтинг недостаточен для этого заказа (требуется ${order.minWorkerRating})")
        }
        
        // Проверка, не взял ли уже грузчик этот заказ
        val alreadyTaken = orderRepository.hasWorkerTakenOrder(params.orderId, params.workerId)
        if (alreadyTaken) {
            return Result.Error("Вы уже взяли этот заказ")
        }
        
        // Проверка лимита грузчиков
        val currentWorkerCount = orderRepository.getWorkerCountSync(params.orderId)
        if (currentWorkerCount >= order.requiredWorkers) {
            return Result.Error("Все места на заказе заняты")
        }
        
        // Взятие заказа
        return orderRepository.takeOrder(params.orderId, params.workerId)
    }
}
