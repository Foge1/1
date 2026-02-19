package com.loaderapp.core.common

/**
 * Базовый sealed class для состояний UI.
 * Используется в ViewModels для управления состоянием экранов.
 */
sealed class UiState<out T> {
    /**
     * Начальное состояние (ничего не загружено)
     */
    object Idle : UiState<Nothing>()
    
    /**
     * Загрузка данных
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Успешная загрузка с данными
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Ошибка загрузки
     */
    data class Error(val message: String) : UiState<Nothing>()
    
    /**
     * Проверка состояний
     */
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isIdle: Boolean get() = this is Idle
    
    /**
     * Получить данные или null
     */
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

/**
 * Extension для map операции над UiState
 */
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Error -> this
        is UiState.Loading -> this
        is UiState.Idle -> this
    }
}

/**
 * Extension для обработки успеха
 */
inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) {
        action(data)
    }
    return this
}

/**
 * Extension для обработки ошибки
 */
inline fun <T> UiState<T>.onError(action: (String) -> Unit): UiState<T> {
    if (this is UiState.Error) {
        action(message)
    }
    return this
}

/**
 * Extension для обработки загрузки
 */
inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) {
        action()
    }
