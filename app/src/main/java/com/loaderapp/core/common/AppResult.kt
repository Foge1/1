package com.loaderapp.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Unified app-level operation result.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** Executes [block] and converts thrown exceptions to [AppResult.Failure]. */
inline fun <T> appRunCatching(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (throwable: Throwable) {
    AppResult.Failure(throwable.toAppError())
}

/** Maps successful value and keeps failure as-is. */
inline fun <T, R> AppResult<T>.mapResult(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** Converts a plain data [Flow] into [Flow] of [AppResult]. */
fun <T> Flow<T>.asResult(): Flow<AppResult<T>> =
    map<T, AppResult<T>> { AppResult.Success(it) }
        .catch { emit(AppResult.Failure(it.toAppError())) }

/** Maps success values inside [Flow] of [AppResult]. */
inline fun <T, R> Flow<AppResult<T>>.mapResult(
    crossinline transform: (T) -> R
): Flow<AppResult<R>> = map { result ->
    result.mapResult(transform)
}
