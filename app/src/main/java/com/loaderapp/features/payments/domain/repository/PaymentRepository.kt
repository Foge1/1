package com.loaderapp.features.payments.domain.repository

import com.loaderapp.features.payments.domain.model.PaymentModel
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория платежей.
 * TODO(TECH-DEBT-002): Реализовать PaymentRepositoryImpl c интеграцией платёжного шлюза, идемпотентностью
 * запросов и обработкой ошибок провайдера; done when feature покрыта e2e и используется в production path.
 */
interface PaymentRepository {
    suspend fun createPayment(payment: PaymentModel): Result<PaymentModel>

    suspend fun getPaymentByOrder(orderId: Long): PaymentModel?

    fun getPaymentsByWorker(workerId: Long): Flow<List<PaymentModel>>

    suspend fun confirmPayment(paymentId: Long): Result<Unit>

    suspend fun refundPayment(paymentId: Long): Result<Unit>
}
