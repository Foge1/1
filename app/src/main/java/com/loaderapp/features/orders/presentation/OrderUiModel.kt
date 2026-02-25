package com.loaderapp.features.orders.presentation

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderActionBlockReason
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.toDisplayMessage

data class OrderUiModel(
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
) {
    val applyDisabledReason: String?
        get() = applyBlockReason?.toDisplayMessage()

    val withdrawDisabledReason: String?
        get() = withdrawBlockReason?.toDisplayMessage()

    val startDisabledReason: String?
        get() = startBlockReason?.toDisplayMessage()

    val cancelDisabledReason: String?
        get() = cancelBlockReason?.toDisplayMessage()

    val completeDisabledReason: String?
        get() = completeBlockReason?.toDisplayMessage()

    val myApplication: OrderApplication?
        get() = order.applications.firstOrNull { it.loaderId == currentUserId }

    val myApplicationStatus: OrderApplicationStatus?
        get() = myApplication?.status

    val selectedApplicantsCount: Int
        get() = order.applications.count { it.status == OrderApplicationStatus.SELECTED }

    val appliedApplicantsCount: Int
        get() = order.applications.count { it.status == OrderApplicationStatus.APPLIED }

    val visibleApplicants: List<OrderApplication>
        get() = order.applications.filter {
            it.status == OrderApplicationStatus.APPLIED ||
                it.status == OrderApplicationStatus.SELECTED
        }
}
