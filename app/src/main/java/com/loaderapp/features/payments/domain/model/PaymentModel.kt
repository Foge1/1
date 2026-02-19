package com.loaderapp.features.payments.domain.model

/**
 * Domain-модель платежа.
 * TODO: Добавить Room-entity и маппер когда будет реализован модуль оплаты.
 */
data class PaymentModel(
    val id: Long = 0,
    val orderId: Long,
    val workerId: Long,
    val amount: Double,
    val status: PaymentStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null
)

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED;

    fun getDisplayName(): String = when (this) {
        PENDING -> "Ожидает оплаты"
        COMPLETED -> "Оплачено"
        FAILED -> "Ошибка оплаты"
        REFUNDED -> "Возвращено"
    }
}
