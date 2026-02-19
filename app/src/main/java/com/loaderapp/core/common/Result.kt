package com.loaderapp.core.common

/**
 * Sealed class для представления результата операции.
 * Используется для явной обработки успеха/ошибки без исключений.
 */
sealed class Result<out T> {
    /**
     * Успешный результат с данными
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Ошибка с сообщением и опциональным исключением
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>()
    
    /**
     * Состояние загрузки
     */
    object Loading : Result<Nothing>()
    
    /**
     * Проверка на успех
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Проверка на ошибку
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Получить данные или null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Получить данные или значение по умолчанию
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }
}

/**
 * Extension для map операции над Result
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * Extension для flatMap операции над Result
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * Extension для обработки успеха
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Extension для обработки ошибки
 */
inline fun <T> Result<T>.onError(action: (String, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(message, exception)
    }
    return this
}
