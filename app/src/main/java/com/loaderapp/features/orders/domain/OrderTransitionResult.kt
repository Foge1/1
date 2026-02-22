package com.loaderapp.features.orders.domain

sealed interface OrderTransitionResult {
    data class Success(val order: Order) : OrderTransitionResult
    data class Failure(val reason: String) : OrderTransitionResult
}
