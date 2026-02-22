package com.loaderapp.domain.usecase.chat

import com.loaderapp.core.common.Result
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
    private val ordersRepository: OrdersRepository
) : UseCase<CanAccessOrderChatParams, Boolean>() {

    override suspend fun execute(params: CanAccessOrderChatParams): Result<Boolean> {
        val order = ordersRepository.observeOrders().firstOrNull()
            ?.firstOrNull { it.id == params.orderId }
            ?: return Result.Error("Заказ не найден")

        return Result.Success(order.status == OrderStatus.IN_PROGRESS)
    }
}
