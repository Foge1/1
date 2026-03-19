package com.loaderapp.features.orders.domain.usecase

import kotlinx.coroutines.CancellationException

internal inline fun <T> runCatchingUseCase(
    defaultErrorMessage: String,
    block: () -> T,
): UseCaseResult<T> =
    runCatching(block)
        .fold(
            onSuccess = { UseCaseResult.Success(it) },
            onFailure = { throwable ->
                if (throwable is CancellationException) throw throwable
                UseCaseResult.Failure(throwable.message ?: defaultErrorMessage)
            },
        )
