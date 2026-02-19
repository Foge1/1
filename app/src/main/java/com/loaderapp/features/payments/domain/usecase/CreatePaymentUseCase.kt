package com.loaderapp.features.payments.domain.usecase

import com.loaderapp.features.payments.domain.model.PaymentModel
import com.loaderapp.features.payments.domain.repository.PaymentRepository
import javax.inject.Inject

data class CreatePaymentParams(val orderId: Long, val workerId: Long, val amount: Double)

/**
 * UseCase для создания платежа после завершения заказа.
 * TODO: Реализовать когда будет подключён платёжный шлюз.
 */
class CreatePaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(params: CreatePaymentParams): Result<PaymentModel> {
        val payment = PaymentModel(
            orderId = params.orderId,
            workerId = params.workerId,
            amount = params.amount,
            status = com.loaderapp.features.payments.domain.model.PaymentStatus.PENDING
        )
        return paymentRepository.createPayment(payment)
    }
}
