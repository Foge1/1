package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
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

        val currentUser = currentUserProvider.getCurrentUser()
        val normalizedOrder = order.copy(
            status = OrderStatus.AVAILABLE,
            createdByUserId = currentUser.id,
            acceptedByUserId = null,
            acceptedAtMillis = null
        )

        return runCatching {
            ordersRepository.createOrder(normalizedOrder)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось создать заказ")
        }
    }
}
