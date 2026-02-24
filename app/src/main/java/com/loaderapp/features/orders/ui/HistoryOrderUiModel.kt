package com.loaderapp.features.orders.ui

data class HistoryOrderUiModel(
    val id: Long,
    val timestampMillis: Long,
    val address: String,
    val searchableDetails: String,
    val order: OrderUiModel
)

fun OrderUiModel.toHistoryOrderUiModel(): HistoryOrderUiModel {
    val details = buildString {
        append(order.title)
        if (!order.comment.isNullOrBlank()) {
            append(' ')
            append(order.comment)
        }
        if (order.tags.isNotEmpty()) {
            append(' ')
            append(order.tags.joinToString(" "))
        }
    }

    return HistoryOrderUiModel(
        id = order.id,
        timestampMillis = order.dateTime,
        address = order.address,
        searchableDetails = details,
        order = this
    )
}
