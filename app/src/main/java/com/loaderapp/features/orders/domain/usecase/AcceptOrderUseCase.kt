package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject

/**
 * @Deprecated Используйте [ApplyToOrderUseCase] + [SelectApplicantUseCase] + [StartOrderUseCase].
 *
 * Оставлен как тонкий compat-shim для старых call-site'ов.
 * Не запускает заказ и не переводит его в IN_PROGRESS.
 */
@Deprecated(
    message = "Use ApplyToOrderUseCase. This shim will be removed in Step 5.",
    replaceWith = ReplaceWith("ApplyToOrderUseCase(orderId)")
)
class AcceptOrderUseCase @Inject constructor(
    private val repository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val applyToOrderUseCase: ApplyToOrderUseCase
) {
    @Suppress("DEPRECATION")
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> {
        val actor = currentUserProvider.getCurrentUser()
        if (actor.role != Role.LOADER) {
            return UseCaseResult.Failure("Только грузчик может откликнуться на заказ")
        }

        val order = repository.getOrderById(orderId)
            ?: return UseCaseResult.Failure("Заказ не найден")

        if (order.status != OrderStatus.STAFFING) {
            return UseCaseResult.Failure("Заказ должен быть в статусе STAFFING")
        }

        if (order.workersTotal != 1) {
            return UseCaseResult.Failure("Legacy accept доступен только для single-worker заказа")
        }

        return applyToOrderUseCase(orderId)
    }
}
