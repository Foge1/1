package com.loaderapp.domain.model

/**
 * Domain модель сообщения чата
 */
data class ChatMessageModel(
    val id: Long,
    val orderId: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: UserRoleModel,
    val text: String,
    val sentAt: Long
)
