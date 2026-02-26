package com.loaderapp.features.orders.domain.usecase

import kotlinx.coroutines.CancellationException

internal inline fun <T> runCatchingUseCase(
    defaultErrorMessage: String,
    block: () -> T
): UseCaseResult<T> {
    return try {
        UseCaseResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UseCaseResult.Failure(e.message ?: defaultErrorMessage)
    }
}
