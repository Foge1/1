package com.loaderapp.features.orders.domain.usecase

import javax.inject.Inject

/**
 * @Deprecated Используйте [ApplyToOrderUseCase] + [SelectApplicantUseCase] + [StartOrderUseCase].
 *
 * Оставлен как тонкий делегат исключительно для сохранения компиляции call-site'ов,
 * которые будут удалены в Step 5 (UI-рефакторинг).
 * Не содержит собственной бизнес-логики.
 */
@Deprecated(
    message = "Use ApplyToOrderUseCase. This shim will be removed in Step 5.",
    replaceWith = ReplaceWith("ApplyToOrderUseCase(orderId)")
)
class AcceptOrderUseCase @Inject constructor(
    private val applyToOrderUseCase: ApplyToOrderUseCase
) {
    @Suppress("DEPRECATION")
    suspend operator fun invoke(orderId: Long): UseCaseResult<Unit> =
        applyToOrderUseCase(orderId)
}
