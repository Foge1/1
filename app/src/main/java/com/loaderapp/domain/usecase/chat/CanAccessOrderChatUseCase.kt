package com.loaderapp.domain.usecase.chat

import com.loaderapp.core.common.Result
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.base.UseCase
import com.loaderapp.features.orders.data.OrdersRepository
import com.loaderapp.features.orders.domain.OrderStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

data class CanAccessOrderChatParams(
    val orderId: Long,
    val userId: Long
)

class CanAccessOrderChatUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val ordersRepository: OrdersRepository
) : UseCase<CanAccessOrderChatParams, Boolean>() {

    override suspend fun execute(params: CanAccessOrderChatParams): Result<Boolean> {
        val featureOrder = ordersRepository.observeOrders().firstOrNull()
            ?.firstOrNull { it.id == params.orderId }

        if (featureOrder != null) {
            val statusAllowed = featureOrder.status == OrderStatus.IN_PROGRESS
            if (!statusAllowed) return Result.Success(false)

            val dispatcherId = featureOrder.meta[DISPATCHER_ID_KEY]?.toLongOrNull()
            val isDispatcher = dispatcherId == params.userId
            return Result.Success(isDispatcher || orderRepository.hasWorkerTakenOrder(featureOrder.id, params.userId))
        }

        val order = when (val result = orderRepository.getOrderById(params.orderId)) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error(result.message, result.exception)
            is Result.Loading -> return Result.Loading
        }

        val statusAllowed = order.status.name == TAKEN_STATUS || order.status.name == IN_PROGRESS_STATUS
        if (!statusAllowed) return Result.Success(false)

        val isDispatcher = order.dispatcherId == params.userId
        val isAssignedWorker = order.workerId == params.userId ||
            orderRepository.hasWorkerTakenOrder(order.id, params.userId)

        return Result.Success(isDispatcher || isAssignedWorker)
    }

    private companion object {
        const val DISPATCHER_ID_KEY = "dispatcherId"
        const val TAKEN_STATUS = "TAKEN"
        const val IN_PROGRESS_STATUS = "IN_PROGRESS"
    }
}
