package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

@Deprecated("Step-2 compatibility adapter. Use ApplyToOrderUseCase in step-3")
class AcceptOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val currentUser = currentUserProvider.getCurrentUser()
        return runCatching {
            ordersRepository.applyToOrder(orderId, currentUser.id, System.currentTimeMillis())
            UseCaseResult.Success(Unit)
        }.getOrElse { error ->
            UseCaseResult.Failure(error.message ?: "Не удалось принять заказ")
        }
    }
}
