package com.loaderapp.presentation.loader

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.usecase.order.*
import com.loaderapp.domain.usecase.user.GetUserByIdParams
import com.loaderapp.domain.usecase.user.GetUserByIdUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана грузчика
 * Использует UseCases для бизнес-логики
 */
@HiltViewModel
class LoaderViewModel @Inject constructor(
    private val getAvailableOrdersUseCase: GetAvailableOrdersUseCase,
    private val getOrdersByWorkerUseCase: GetOrdersByWorkerUseCase,
    private val takeOrderUseCase: TakeOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase
) : BaseViewModel() {
    
    /**
     * ID текущего грузчика
     */
    private var workerId: Long = 0
    
    /**
     * Состояние доступных заказов
     */
    private val _availableOrdersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Idle)
    val availableOrdersState: StateFlow<UiState<List<OrderModel>>> = _availableOrdersState.asStateFlow()
    
    /**
     * Состояние моих заказов
     */
    private val _myOrdersState = MutableStateFlow<UiState<List<OrderModel>>>(UiState.Idle)
    val myOrdersState: StateFlow<UiState<List<OrderModel>>> = _myOrdersState.asStateFlow()
    
    /**
     * Состояние статистики
     */
    private val _statsState = MutableStateFlow<UiState<WorkerStats>>(UiState.Idle)
    val statsState: StateFlow<UiState<WorkerStats>> = _statsState.asStateFlow()
    
    /**
     * Состояние обновления (pull-to-refresh)
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    /**
     * Количество грузчиков на каждом заказе
     */
    private val _workerCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val workerCounts: StateFlow<Map<Long, Int>> = _workerCounts.asStateFlow()
    
    /**
     * Инициализация с ID грузчика
     */
    fun initialize(workerId: Long) {
        this.workerId = workerId
        loadAvailableOrders()
        loadMyOrders()
        loadStats()
    }
    
    /**
     * Загрузить доступные заказы
     * Фильтрует по рейтингу грузчика автоматически
     */
    private fun loadAvailableOrders() {
        viewModelScope.launch {
            _availableOrdersState.setLoading()
            
            try {
                // Получаем текущего пользователя для фильтрации по рейтингу
                val userResult = getUserByIdUseCase(GetUserByIdParams(workerId))
                val userRating = when (userResult) {
                    is com.loaderapp.core.common.Result.Success -> userResult.data.rating.toFloat()
                    else -> 5.0f
                }
                
                // Загружаем доступные заказы и фильтруем по рейтингу
                getAvailableOrdersUseCase(Unit)
                    .map { orders ->
                        orders.filter { it.isAvailableForWorker(userRating) }
                    }
                    .collect { filteredOrders ->
                        _availableOrdersState.setSuccess(filteredOrders)
                        // TODO: Загрузить workerCounts
                    }
            } catch (e: Exception) {
                _availableOrdersState.setError("Ошибка загрузки доступных заказов")
            }
        }
    }
    
    /**
     * Загрузить мои заказы
     */
    private fun loadMyOrders() {
        viewModelScope.launch {
            _myOrdersState.setLoading()
            
            try {
                getOrdersByWorkerUseCase(GetOrdersByWorkerParams(workerId))
                    .collect { orders ->
                        _myOrdersState.setSuccess(orders)
                    }
            } catch (e: Exception) {
                _myOrdersState.setError("Ошибка загрузки моих заказов")
            }
        }
    }
    
    /**
     * Загрузить статистику
     */
    private fun loadStats() {
        viewModelScope.launch {
            _statsState.setLoading()
            
            try {
                getWorkerStatsUseCase(GetWorkerStatsParams(workerId))
                    .collect { stats ->
                        _statsState.setSuccess(stats)
                    }
            } catch (e: Exception) {
                _statsState.setError("Ошибка загрузки статистики")
            }
        }
    }
    
    /**
     * Взять заказ
     */
    fun takeOrder(order: OrderModel, onSuccess: () -> Unit = {}) {
        launchSafe {
            val result = takeOrderUseCase(TakeOrderParams(order.id, workerId))
            
            when (result) {
                is com.loaderapp.core.common.Result.Success -> {
                    showSnackbar("Заказ успешно взят")
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
     * Завершить заказ
     */
    fun completeOrder(order: OrderModel, onSuccess: () -> Unit = {}) {
        launchSafe {
            val result = completeOrderUseCase(CompleteOrderParams(order.id))
            
            when (result) {
                is com.loaderapp.core.common.Result.Success -> {
                    showSnackbar("Заказ завершён")
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
     * Обновить данные (pull-to-refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            try {
                // Перезагружаем все данные
                loadAvailableOrders()
                loadMyOrders()
                loadStats()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
