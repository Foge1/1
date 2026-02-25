package com.loaderapp.core.common

/**
 * Legacy result model kept for gradual migration.
 * New code should use [AppResult].
 */
@Deprecated(
    message = "Use AppResult instead",
    replaceWith = ReplaceWith("AppResult")
)
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val error: AppError,
        val exception: Throwable? = null
    ) : Result<Nothing>() {
        constructor(message: String, exception: Throwable? = null) : this(
            error = AppError.Validation(message = message),
            exception = exception
        )

        val message: String
            get() = when (val current = error) {
                is AppError.Validation -> current.message ?: "Ошибка валидации"
                is AppError.Backend -> current.serverMessage ?: "Ошибка сервера"
                AppError.Auth.Forbidden -> "Доступ запрещён"
                AppError.Auth.SessionExpired -> "Сессия истекла"
                AppError.Auth.Unauthorized -> "Требуется авторизация"
                AppError.Network.Dns -> "DNS ошибка"
                AppError.Network.NoInternet -> "Нет интернета"
                AppError.Network.Timeout -> "Превышено время ожидания"
                AppError.Network.UnknownHost -> "Неизвестный хост"
                AppError.NotFound -> "Не найдено"
                is AppError.Storage.Db -> "Ошибка базы данных"
                is AppError.Storage.Serialization -> "Ошибка сериализации"
                is AppError.Unknown -> current.cause?.message ?: "Неизвестная ошибка"
            }
    }

    object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
    is Result.Loading -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (AppError, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(error, exception)
    return this
}

fun <T> Result<T>.toAppResult(): AppResult<T> = when (this) {
    is Result.Success -> AppResult.Success(data)
    is Result.Error -> AppResult.Failure(error)
    is Result.Loading -> AppResult.Failure(AppError.Unknown())
}

fun <T> AppResult<T>.toLegacyResult(): Result<T> = when (this) {
    is AppResult.Success -> Result.Success(data)
    is AppResult.Failure -> Result.Error(error)
}
