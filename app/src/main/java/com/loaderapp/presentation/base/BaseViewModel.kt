package com.loaderapp.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.core.common.UiState
import com.loaderapp.core.common.UiText
import com.loaderapp.presentation.common.toUiText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing coroutine safety and snackbar messaging.
 *
 * ## Snackbar via Channel, not SharedFlow
 * [MutableSharedFlow] without `replay` loses events if the UI is not yet
 * subscribed at the moment of emission — a real risk during screen transitions.
 * [Channel.BUFFERED] guarantees delivery to exactly one collector and buffers
 * up to 64 undelivered messages, which is the correct pattern for one-shot UI
 * events in Compose.
 */
abstract class BaseViewModel : ViewModel() {

    private val _snackbarMessage = Channel<UiText>(Channel.BUFFERED)

    /** Collect in UI with [LaunchedEffect] to show Snackbar messages. */
    val snackbarMessage = _snackbarMessage.receiveAsFlow()

    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    protected open fun handleError(throwable: Throwable) {
        showSnackbar(UiText.Dynamic(throwable.message ?: "Произошла неизвестная ошибка"))
    }

    protected fun showSnackbar(message: String) {
        showSnackbar(UiText.Dynamic(message))
    }

    protected fun showSnackbar(message: UiText) {
        viewModelScope.launch { _snackbarMessage.send(message) }
    }

    /** Launch a coroutine with [exceptionHandler] attached. */
    protected fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) { block() }
    }

    // ── StateFlow extension helpers ───────────────────────────────────────────

    protected fun <T> MutableStateFlow<UiState<T>>.setLoading() {
        value = UiState.Loading
    }

    protected fun <T> MutableStateFlow<UiState<T>>.setSuccess(data: T) {
        value = UiState.Success(data)
    }

    protected fun <T> MutableStateFlow<UiState<T>>.setError(message: String) {
        setError(UiText.Dynamic(message))
    }

    protected fun <T> MutableStateFlow<UiState<T>>.setError(message: UiText) {
        value = UiState.Error(message)
        showSnackbar(message)
    }

    /**
     * Handle a [Result] and update [stateFlow] accordingly.
     * Prefer this over manual when-expressions in subclasses.
     */
    protected fun <T> handleResult(
        result: Result<T>,
        stateFlow: MutableStateFlow<UiState<T>>,
        onSuccess: ((T) -> Unit)? = null
    ) {
        when (result) {
            is Result.Success -> { stateFlow.setSuccess(result.data); onSuccess?.invoke(result.data) }
            is Result.Error   -> stateFlow.setError(result.error.toUiText())
            is Result.Loading -> stateFlow.setLoading()
        }
    }
}
