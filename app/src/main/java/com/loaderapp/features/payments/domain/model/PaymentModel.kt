package com.loaderapp.features.payments.domain.model

/**
 * Domain-модель платежа.
 * TODO(TECH-DEBT-003): Добавить Room entity и мапперы для хранения платежей локально; done when платежи
 * кэшируются в БД и синхронизируются с backend source of truth.
 */
data class PaymentModel(
    val id: Long = 0,
    val orderId: Long,
    val workerId: Long,
    val amount: Double,
    val status: PaymentStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
)

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    ;

    fun getDisplayName(): String =
        when (this) {
            PENDING -> "Ожидает оплаты"
            COMPLETED -> "Оплачено"
            FAILED -> "Ошибка оплаты"
            REFUNDED -> "Возвращено"
        }
}
