package com.loaderapp.core.contract

interface VersionedEntity {
    val id: String
    val version: Int
    val updatedAt: String
}

enum class Role {
    DISPATCHER,
    LOADER,
}

enum class OrderStatus {
    CREATED,
    PUBLISHED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class OrderTransitionType {
    PUBLISH,
    ASSIGN_LOADER,
    START_IN_PROGRESS,
    COMPLETE,
    CANCEL,
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any?>? = null,
    val traceId: String? = null,
)

data class CursorPage<T>(
    val items: List<T>,
    val limit: Int,
    val nextCursor: String?,
)
