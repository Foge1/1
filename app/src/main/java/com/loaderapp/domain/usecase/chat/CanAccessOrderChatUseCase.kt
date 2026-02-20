package com.loaderapp.domain.usecase.chat

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

data class CanAccessOrderChatParams(
    val orderId: Long,
    val userId: Long
)

class CanAccessOrderChatUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) : UseCase<CanAccessOrderChatParams, Boolean>() {

    override suspend fun execute(params: CanAccessOrderChatParams): Result<Boolean> {
        val order = when (val result = orderRepository.getOrderById(params.orderId)) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error(result.message, result.exception)
            is Result.Loading -> return Result.Loading
        }

        val statusAllowed = order.status == OrderStatusModel.TAKEN || order.status == OrderStatusModel.IN_PROGRESS
        if (!statusAllowed) return Result.Success(false)

        val isDispatcher = order.dispatcherId == params.userId
        val isAssignedWorker = order.workerId == params.userId ||
            orderRepository.hasWorkerTakenOrder(order.id, params.userId)

        return Result.Success(isDispatcher || isAssignedWorker)
    }
}
