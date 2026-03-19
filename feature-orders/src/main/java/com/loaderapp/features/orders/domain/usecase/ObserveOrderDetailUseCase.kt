package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.model.OrderDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveOrderDetailUseCase
    @Inject
    constructor(
        private val observeOrderUiModelsUseCase: ObserveOrderUiModelsUseCase,
    ) {
        operator fun invoke(orderId: Long): Flow<ObserveOrderDetailResult> =
            observeOrderUiModelsUseCase().map { result ->
                when (result) {
                    ObserveOrderUiModelsResult.NotSelected -> ObserveOrderDetailResult.NotSelected
                    is ObserveOrderUiModelsResult.Selected -> {
                        val order = result.orders.firstOrNull { it.order.id == orderId }
                        if (order == null) {
                            ObserveOrderDetailResult.NotFound
                        } else {
                            ObserveOrderDetailResult.Success(order)
                        }
                    }
                }
            }
    }

sealed interface ObserveOrderDetailResult {
    data object NotSelected : ObserveOrderDetailResult

    data object NotFound : ObserveOrderDetailResult

    data class Success(
        val order: OrderDetail,
    ) : ObserveOrderDetailResult
}
