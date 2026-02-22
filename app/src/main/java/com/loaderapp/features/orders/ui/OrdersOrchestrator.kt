package com.loaderapp.features.orders.ui

import android.util.Log
import com.loaderapp.features.orders.domain.usecase.AcceptOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import javax.inject.Inject

class OrdersOrchestrator @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val acceptOrderUseCase: AcceptOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
    private val refreshOrdersUseCase: RefreshOrdersUseCase
) {

    suspend fun execute(command: OrdersCommand): UseCaseResult<Unit> {
        log("execute: $command")
        val result = when (command) {
            is OrdersCommand.Refresh -> refreshOrdersUseCase()
            is OrdersCommand.Create -> createOrderUseCase(command.orderDraft)
            is OrdersCommand.Accept -> acceptOrderUseCase(command.orderId)
            is OrdersCommand.Cancel -> cancelOrderUseCase(command.orderId, command.reason)
            is OrdersCommand.Complete -> completeOrderUseCase(command.orderId)
        }

        when (result) {
            is UseCaseResult.Success -> log("success: $command")
            is UseCaseResult.Failure -> log("failure: $command, reason=${result.reason}")
        }

        return result
    }

    private fun log(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private companion object {
        const val TAG = "OrdersOrchestrator"
    }
}
