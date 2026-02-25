package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import javax.inject.Inject

class RefreshOrdersUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(): UseCaseResult<Unit> {
        return runCatching {
            ordersRepository.refresh()
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось обновить заказы")
        }
    }
}
