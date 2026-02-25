package com.loaderapp.features.orders.presentation

import com.loaderapp.features.orders.domain.usecase.ApplyToOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CancelOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CompleteOrderUseCase
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import com.loaderapp.features.orders.domain.usecase.RefreshOrdersUseCase
import com.loaderapp.features.orders.domain.usecase.SelectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.StartOrderUseCase
import com.loaderapp.features.orders.domain.usecase.UnselectApplicantUseCase
import com.loaderapp.features.orders.domain.usecase.UseCaseResult
import com.loaderapp.features.orders.domain.usecase.WithdrawApplicationUseCase
import com.loaderapp.core.logging.AppLogger
import javax.inject.Inject

class OrdersOrchestrator @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val applyToOrderUseCase: ApplyToOrderUseCase,
    private val withdrawApplicationUseCase: WithdrawApplicationUseCase,
    private val selectApplicantUseCase: SelectApplicantUseCase,
    private val unselectApplicantUseCase: UnselectApplicantUseCase,
    private val startOrderUseCase: StartOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
    private val refreshOrdersUseCase: RefreshOrdersUseCase,
    private val appLogger: AppLogger
) {

    suspend fun execute(command: OrdersCommand): UseCaseResult<Unit> {
        log("execute: $command")
        val result = dispatch(command)
        when (result) {
            is UseCaseResult.Success -> log("success: $command")
            is UseCaseResult.Failure -> log("failure: $command, reason=${result.reason}")
        }
        return result
    }

    private suspend fun dispatch(command: OrdersCommand): UseCaseResult<Unit> = when (command) {
        is OrdersCommand.Refresh -> refreshOrdersUseCase()
        is OrdersCommand.Create -> createOrderUseCase(command.orderDraft)
        is OrdersCommand.Apply -> applyToOrderUseCase(command.orderId)
        is OrdersCommand.Withdraw -> withdrawApplicationUseCase(command.orderId)
        is OrdersCommand.Select -> selectApplicantUseCase(command.orderId, command.loaderId)
        is OrdersCommand.Unselect -> unselectApplicantUseCase(command.orderId, command.loaderId)
        is OrdersCommand.Start -> startOrderUseCase(command.orderId)
        is OrdersCommand.Cancel -> cancelOrderUseCase(command.orderId, command.reason)
        is OrdersCommand.Complete -> completeOrderUseCase(command.orderId)
    }

    private fun log(message: String) {
        appLogger.d(TAG, message)
    }

    private companion object {
        const val TAG = "OrdersOrchestrator"
    }
}
