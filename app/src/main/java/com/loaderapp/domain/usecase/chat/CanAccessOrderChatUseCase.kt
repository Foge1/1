package com.loaderapp.domain.usecase.chat

import com.loaderapp.core.common.Result
import com.loaderapp.domain.usecase.base.UseCase
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import javax.inject.Inject

data class CanAccessOrderChatParams(
    val orderId: Long,
    val userId: Long
)

class CanAccessOrderChatUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) : UseCase<CanAccessOrderChatParams, Boolean>() {

    override suspend fun execute(params: CanAccessOrderChatParams): Result<Boolean> {
        val order = ordersRepository.getOrderById(params.orderId)
            ?: return Result.Success(false)

        return Result.Success(order.status == OrderStatus.IN_PROGRESS)
    }
}
