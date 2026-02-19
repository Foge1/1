package com.loaderapp.domain.usecase.order

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

data class GetOrderByIdParams(val orderId: Long)

class GetOrderByIdUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : UseCase<GetOrderByIdParams, OrderModel>() {
    override suspend fun execute(params: GetOrderByIdParams): Result<OrderModel> =
        orderRepository.getOrderById(params.orderId)
}
