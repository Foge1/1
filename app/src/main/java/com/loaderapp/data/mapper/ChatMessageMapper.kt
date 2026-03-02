package com.loaderapp.data.mapper

import com.loaderapp.data.model.ChatMessage
import com.loaderapp.domain.model.ChatMessageModel

/**
 * Mapper для конвертации ChatMessage между data и domain слоями
 */
object ChatMessageMapper {
    /**
     * Конвертация Data Entity -> Domain Model
     */
    fun toDomain(entity: ChatMessage): ChatMessageModel =
        ChatMessageModel(
            id = entity.id,
            orderId = entity.orderId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            senderRole =
                UserMapper
                    .toDomain(
                        com.loaderapp.data.model.User(
                            id = 0,
                            name = "",
                            phone = "",
                            role = entity.senderRole,
                            rating = 0.0,
                        ),
                    ).role,
            text = entity.text,
            sentAt = entity.sentAt,
        )

    /**
     * Конвертация Domain Model -> Data Entity
     */
    fun toEntity(model: ChatMessageModel): ChatMessage =
        ChatMessage(
            id = model.id,
            orderId = model.orderId,
            senderId = model.senderId,
            senderName = model.senderName,
            senderRole =
                UserMapper
                    .toEntity(
                        com.loaderapp.domain.model.UserModel(
                            id = 0,
                            name = "",
                            phone = "",
                            role = model.senderRole,
                            rating = 0.0,
                            birthDate = null,
                            avatarInitials = "",
                            createdAt = 0,
                        ),
                    ).role,
            text = model.text,
            sentAt = model.sentAt,
        )

    /**
     * Конвертация списка Entity -> Domain
     */
    fun toDomainList(entities: List<ChatMessage>): List<ChatMessageModel> = entities.map { toDomain(it) }

    /**
     * Конвертация списка Domain -> Entity
     */
    fun toEntityList(models: List<ChatMessageModel>): List<ChatMessage> = models.map { toEntity(it) }
}
