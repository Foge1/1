package com.loaderapp.features.orders.domain

sealed class OrderTransitionResult {
    data class Success(val order: Order) : OrderTransitionResult()
    data class Failure(val reason: String) : OrderTransitionResult()
}
