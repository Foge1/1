package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Параметры для поиска заказов
 */
data class SearchOrdersParams(
    val query: String,
    val status: OrderStatusModel? = null
)

/**
 * UseCase: Поиск заказов по запросу
 */
class SearchOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<SearchOrdersParams, Flow<List<OrderModel>>>() {
    
    override fun execute(params: SearchOrdersParams): Flow<List<OrderModel>> {
        return orderRepository.searchOrders(params.query, params.status)
    }
}
