package com.loaderapp.presentation.dispatcher

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.*
import com.loaderapp.domain.usecase.user.GetUserByIdParams
import com.loaderapp.domain.usecase.user.GetUserByIdUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана диспетчера
 * Использует UseCases для бизнес-логики
 * 
 * @HiltViewModel - автоматическая инъекция зависимостей через Hilt
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DispatcherViewModel @Inject constructor(
    private val getOrdersByDispatcherUseCase: GetOrdersByDispatcherUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val searchOrdersUseCase: SearchOrdersUseCase,
    private val getDispatcherStatsUseCase: GetDispatcherStatsUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase
) : BaseViewModel() {
    
    /**
     * ID текущего диспетчера (устанавливается извне)
     */
    private var dispatcherId: Long = 0
    
    /**
     * Состояние списка заказов
     */
    private val _ordersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Idle)
    val ordersState: StateFlow<UiState<List<OrderModel>>> = _ordersState.asStateFlow()
    
    /**
     * Поисковый запрос
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /**
     * Активен ли поиск
     */
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()
    
    /**
     * Состояние статистики
     */
    private val _statsState = MutableStateFlow<UiState<DispatcherStats>>(UiState.Idle)
    val statsState: StateFlow<UiState<DispatcherStats>> = _statsState.asStateFlow()
    
    /**
     * Количество грузчиков на каждом заказе
     */
    private val _workerCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val workerCounts: StateFlow<Map<Long, Int>> = _workerCounts.asStateFlow()
    
    /**
     * Инициализация ViewModel с ID диспетчера
     */
    fun initialize(dispatcherId: Long) {
        this.dispatcherId = dispatcherId
        observeOrders()
        loadStats()
    }
    
    /**
     * Наблюдение за заказами с поддержкой поиска
     */
    private fun observeOrders() {
        viewModelScope.launch {
            _ordersState.setLoading()
            
            try {
                _searchQuery
                    .debounce(300) // Задержка для оптимизации поиска
                    .flatMapLatest { query ->
                        if (query.isBlank()) {
                            // Все заказы диспетчера
                            getOrdersByDispatcherUseCase(GetOrdersByDispatcherParams(dispatcherId))
                        } else {
                            // Поиск по заказам диспетчера
                            // TODO: Нужен отдельный UseCase для поиска по диспетчеру
                            getOrdersByDispatcherUseCase(GetOrdersByDispatcherParams(dispatcherId))
                                .map { orders ->
                                    orders.filter {
                                        it.address.contains(query, ignoreCase = true) ||
                                        it.cargoDescription.contains(query, ignoreCase = true)
                                    }
                                }
                        }
                    }
                    .collect { orders ->
                        _ordersState.setSuccess(orders)
                        // TODO: Загрузить workerCounts для каждого заказа
                    }
            } catch (e: Exception) {
                _ordersState.setError("Ошибка загрузки заказов: ${e.message}")
            }
        }
    }
    
    /**
     * Загрузить статистику диспетчера
     */
    private fun loadStats() {
        viewModelScope.launch {
            _statsState.setLoading()
            
            try {
                getDispatcherStatsUseCase(GetDispatcherStatsParams(dispatcherId))
                    .collect { stats ->
                        _statsState.setSuccess(stats)
                    }
            } catch (e: Exception) {
                _statsState.setError("Ошибка загрузки статистики")
            }
        }
    }
    
    /**
     * Создать новый заказ
     */
    fun createOrder(order: OrderModel, onSuccess: () -> Unit = {}) {
        launchSafe {
            val result = createOrderUseCase(CreateOrderParams(order))
            
            when (result) {
                is com.loaderapp.core.common.Result.Success -> {
                    showSnackbar("Заказ создан успешно")
                    onSuccess()
                }
                is com.loaderapp.core.common.Result.Error -> {
                    showSnackbar(result.message)
                }
                else -> {}
            }
        }
    }
    
    /**
     * Отменить заказ
     */
    fun cancelOrder(order: OrderModel, onSuccess: () -> Unit = {}) {
        launchSafe {
            val result = cancelOrderUseCase(CancelOrderParams(order.id))
            
            when (result) {
                is com.loaderapp.core.common.Result.Success -> {
                    showSnackbar("Заказ отменён")
                    onSuccess()
                }
                is com.loaderapp.core.common.Result.Error -> {
                    showSnackbar(result.message)
                }
                else -> {}
            }
        }
    }
    
    /**
     * Обновить поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Установить активность поиска
     */
    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }
}
