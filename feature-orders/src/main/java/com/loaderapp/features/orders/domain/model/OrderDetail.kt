package com.loaderapp.features.orders.domain.model

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderActionBlockReason
import com.loaderapp.features.orders.domain.Role

data class OrderDetail(
    val order: Order,
    val currentUserId: String,
    val currentUserRole: Role,
    val canApply: Boolean,
    val applyBlockReason: OrderActionBlockReason?,
    val canWithdraw: Boolean,
    val withdrawBlockReason: OrderActionBlockReason?,
    val canSelect: Boolean,
    val canUnselect: Boolean,
    val canStart: Boolean,
    val startBlockReason: OrderActionBlockReason?,
    val canCancel: Boolean,
    val cancelBlockReason: OrderActionBlockReason?,
    val canComplete: Boolean,
    val completeBlockReason: OrderActionBlockReason?,
    val canOpenChat: Boolean,
)
