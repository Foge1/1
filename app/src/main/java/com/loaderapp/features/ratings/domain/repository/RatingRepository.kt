package com.loaderapp.features.ratings.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория рейтингов.
 * TODO: Реализовать с агрегацией рейтингов по завершённым заказам.
 */
interface RatingRepository {
    suspend fun rateWorker(orderId: Long, workerId: Long, rating: Float, comment: String?): Result<Unit>
    suspend fun rateDispatcher(orderId: Long, dispatcherId: Long, rating: Float): Result<Unit>
    fun getWorkerRating(workerId: Long): Flow<Float>
    fun getWorkerRatingHistory(workerId: Long): Flow<List<RatingEntry>>
}

data class RatingEntry(
    val orderId: Long,
    val rating: Float,
    val comment: String?,
    val createdAt: Long
)
