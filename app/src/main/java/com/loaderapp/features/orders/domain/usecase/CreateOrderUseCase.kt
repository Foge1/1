package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.Order
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(order: Order): UseCaseResult<Unit> {
        if (order.title.isBlank()) {
            return UseCaseResult.Failure("Название заказа не может быть пустым")
        }
        if (order.workersTotal <= 0) {
            return UseCaseResult.Failure("Количество сотрудников должно быть больше нуля")
        }
        if (order.workersCurrent < 0 || order.workersCurrent > order.workersTotal) {
            return UseCaseResult.Failure("Некорректное количество занятых сотрудников")
        }

        return runCatching {
            ordersRepository.createOrder(order)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось создать заказ")
        }
    }
}
