package com.loaderapp.features.orders.presentation.mapper

import com.loaderapp.features.orders.domain.model.OrderDetail
import com.loaderapp.features.orders.presentation.OrderUiModel

fun OrderDetail.toUiModel(): OrderUiModel =
    OrderUiModel(
        order = order,
        currentUserId = currentUserId,
        currentUserRole = currentUserRole,
        canApply = canApply,
        applyBlockReason = applyBlockReason,
        canWithdraw = canWithdraw,
        withdrawBlockReason = withdrawBlockReason,
        canSelect = canSelect,
        canUnselect = canUnselect,
        canStart = canStart,
        startBlockReason = startBlockReason,
        canCancel = canCancel,
        cancelBlockReason = cancelBlockReason,
        canComplete = canComplete,
        completeBlockReason = completeBlockReason,
        canOpenChat = canOpenChat,
    )
