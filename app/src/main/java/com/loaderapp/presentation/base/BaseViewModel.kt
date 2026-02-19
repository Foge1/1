package com.loaderapp.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.core.common.UiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Базовый ViewModel с общей логикой для всех ViewModels
 * 
 * Предоставляет:
 * - Обработку ошибок через CoroutineExceptionHandler
 * - Управление Snackbar сообщениями
 * - Утилиты для запуска корутин
 */
abstract class BaseViewModel : ViewModel() {
    
    /**
     * SharedFlow для Snackbar сообщений (one-time events)
     */
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()
    
    /**
     * Обработчик необработанных исключений в корутинах
     */
    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }
    
    /**
     * Обработка ошибок
     */
    protected open fun handleError(throwable: Throwable) {
        val message = throwable.message ?: "Произошла неизвестная ошибка"
        showSnackbar(message)
    }
    
    /**
     * Показать Snackbar сообщение
     */
    protected fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }
    
    /**
     * Безопасный запуск корутины с обработкой ошибок
     */
    protected fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }
    
    /**
     * Обработать Result и обновить UiState
     */
    protected fun <T> handleResult(
        result: Result<T>,
        stateFlow: MutableStateFlow<UiState<T>>,
        onSuccess: ((T) -> Unit)? = null
    ) {
        when (result) {
            is Result.Success -> {
                stateFlow.value = UiState.Success(result.data)
                onSuccess?.invoke(result.data)
            }
            is Result.Error -> {
                stateFlow.value = UiState.Error(result.message)
                showSnackbar(result.message)
            }
            is Result.Loading -> {
                stateFlow.value = UiState.Loading
            }
        }
    }
    
    /**
     * Установить состояние загрузки
     */
    protected fun <T> MutableStateFlow<UiState<T>>.setLoading() {
        value = UiState.Loading
    }
    
    /**
     * Установить состояние успеха
     */
    protected fun <T> MutableStateFlow<UiState<T>>.setSuccess(data: T) {
        value = UiState.Success(data)
    }
    
    /**
     * Установить состояние ошибки
     */
    protected fun <T> MutableStateFlow<UiState<T>>.setError(message: String) {
        value = UiState.Error(message)
        showSnackbar(message)
    }
}
