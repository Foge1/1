package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderDraft
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderDraft: OrderDraft): UseCaseResult<Unit> {
        if (orderDraft.title.isBlank()) {
            return UseCaseResult.Failure("Название заказа не может быть пустым")
        }
        if (orderDraft.workersTotal <= 0) {
            return UseCaseResult.Failure("Количество сотрудников должно быть больше нуля")
        }
        if (orderDraft.workersCurrent < 0 || orderDraft.workersCurrent > orderDraft.workersTotal) {
            return UseCaseResult.Failure("Некорректное количество занятых сотрудников")
        }

        val currentUser = currentUserProvider.getCurrentUser()
        val order = Order(
            id = 0,
            title = orderDraft.title,
            address = orderDraft.address,
            pricePerHour = orderDraft.pricePerHour,
            orderTime = orderDraft.orderTime,
            durationMin = orderDraft.durationMin,
            workersCurrent = orderDraft.workersCurrent,
            workersTotal = orderDraft.workersTotal,
            tags = orderDraft.tags,
            meta = orderDraft.meta,
            comment = orderDraft.comment,
            status = OrderStatus.STAFFING,
            createdByUserId = currentUser.id
        )

        return runCatching {
            ordersRepository.createOrder(order)
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось создать заказ")
        }
    }
}
