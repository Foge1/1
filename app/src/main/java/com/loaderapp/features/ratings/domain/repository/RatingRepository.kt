package com.loaderapp.features.ratings.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория рейтингов.
 * TODO(TECH-DEBT-006): Реализовать агрегацию рейтингов по завершённым заказам с учётом пересчёта истории;
 * done when getWorkerRating и getWorkerRatingHistory читают согласованные данные из единого источника.
 */
interface RatingRepository {
    suspend fun rateWorker(
        orderId: Long,
        workerId: Long,
        rating: Float,
        comment: String?,
    ): Result<Unit>

    suspend fun rateDispatcher(
        orderId: Long,
        dispatcherId: Long,
        rating: Float,
    ): Result<Unit>

    fun getWorkerRating(workerId: Long): Flow<Float>

    fun getWorkerRatingHistory(workerId: Long): Flow<List<RatingEntry>>
}

data class RatingEntry(
    val orderId: Long,
    val rating: Float,
    val comment: String?,
    val createdAt: Long,
)
