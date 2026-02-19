package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class SearchOrdersByDispatcherParams(val dispatcherId: Long, val query: String)

/**
 * UseCase: Search orders for a specific dispatcher by address or cargo description.
 * Delegates to the DAO SQL query — no in-memory filtering.
 */
class SearchOrdersByDispatcherUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<SearchOrdersByDispatcherParams, Flow<List<OrderModel>>>() {
    override fun execute(params: SearchOrdersByDispatcherParams): Flow<List<OrderModel>> =
        orderRepository.searchOrdersByDispatcher(params.dispatcherId, params.query)
}
