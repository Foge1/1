package com.loaderapp.presentation.dispatcher

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.CreateOrderParams
import com.loaderapp.domain.usecase.order.CreateOrderUseCase
import com.loaderapp.core.common.onError
import com.loaderapp.core.common.onSuccess
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/**
 * ViewModel экрана создания заказа.
 *
 * Намеренно изолирован от [DispatcherViewModel]: экран создания заказа —
 * самостоятельная единица с единственной ответственностью. Результат
 * (созданный заказ) немедленно отражается в [DispatcherViewModel] через
 * Room Flow-подписку, без прямой связи между VM.
 *
 * ## Навигация после успеха
 * Использует [Channel] вместо [SharedFlow] для навигационных событий.
 * Channel гарантирует доставку ровно одному подписчику и не теряет
 * событие при пересоздании UI — это стандарт для one-shot событий
 * в продакшн Compose-приложениях.
 */
@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase
) : BaseViewModel() {

    // Channel для one-shot навигационных событий
    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun createOrder(order: OrderModel) {
        launchSafe {
            createOrderUseCase(CreateOrderParams(order))
                .onSuccess { _ ->
                    showSnackbar("Заказ создан успешно")
                    _navigationEvent.send(NavigationEvent.NavigateUp)
                }
                .onError { msg, _ -> showSnackbar(msg) }
        }
    }
}

sealed class NavigationEvent {
    object NavigateUp : NavigationEvent()
}
