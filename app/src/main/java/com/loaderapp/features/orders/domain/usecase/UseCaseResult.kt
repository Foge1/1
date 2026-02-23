package com.loaderapp.features.orders.domain.usecase

sealed interface UseCaseResult<out T> {
    data class Success<T>(val data: T) : UseCaseResult<T>
    data class Failure(val reason: String) : UseCaseResult<Nothing>
}
