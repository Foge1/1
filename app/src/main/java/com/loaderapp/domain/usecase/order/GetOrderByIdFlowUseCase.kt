package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class GetOrderByIdFlowParams(val orderId: Long)

class GetOrderByIdFlowUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetOrderByIdFlowParams, Flow<OrderModel?>>() {

    override fun execute(params: GetOrderByIdFlowParams): Flow<OrderModel?> {
        return orderRepository.getOrderByIdFlow(params.orderId)
    }
}
