package com.loaderapp.domain.usecase.order

import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class GetWorkerCountParams(val orderId: Long)

class GetWorkerCountUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : FlowUseCase<GetWorkerCountParams, Flow<Int>>() {
    override fun execute(params: GetWorkerCountParams): Flow<Int> =
        orderRepository.getWorkerCountForOrder(params.orderId)
}
