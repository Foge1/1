package com.loaderapp.features.orders.domain

import com.loaderapp.features.orders.domain.session.CurrentUser

object OrderStateMachine {
    data class OrderActions(
        val canAccept: Boolean,
        val canCancel: Boolean,
        val canComplete: Boolean,
        val canOpenChat: Boolean
    )

    fun actionsFor(order: Order): OrderActions {
        return OrderActions(
            canAccept = order.status == OrderStatus.AVAILABLE,
            canCancel = order.status == OrderStatus.AVAILABLE || order.status == OrderStatus.IN_PROGRESS,
            canComplete = order.status == OrderStatus.IN_PROGRESS,
            canOpenChat = order.status == OrderStatus.IN_PROGRESS
        )
    }

    fun transition(order: Order, event: OrderEvent, actor: CurrentUser, now: Long): OrderTransitionResult {
        return when (order.status) {
            OrderStatus.AVAILABLE -> transitionFromAvailable(order, event, actor, now)
            OrderStatus.IN_PROGRESS -> transitionFromInProgress(order, event, actor)
            OrderStatus.COMPLETED,
            OrderStatus.CANCELED,
            OrderStatus.EXPIRED -> OrderTransitionResult.Failure(
                "No transitions allowed from ${order.status}"
            )
        }
    }

    private fun transitionFromAvailable(
        order: Order,
        event: OrderEvent,
        actor: CurrentUser,
        now: Long
    ): OrderTransitionResult {
        return when (event) {
            OrderEvent.ACCEPT -> {
                if (actor.role != Role.LOADER) {
                    OrderTransitionResult.Failure("Only loader can accept order")
                } else {
                    OrderTransitionResult.Success(
                        order.copy(
                            status = OrderStatus.IN_PROGRESS,
                            acceptedByUserId = actor.id,
                            acceptedAtMillis = now
                        )
                    )
                }
            }

            OrderEvent.CANCEL -> {
                if (actor.id != order.createdByUserId) {
                    OrderTransitionResult.Failure("Only creator can cancel available order")
                } else {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
                }
            }

            OrderEvent.EXPIRE -> OrderTransitionResult.Success(
                order.copy(status = OrderStatus.EXPIRED)
            )

            OrderEvent.COMPLETE -> OrderTransitionResult.Failure(
                "Cannot complete order from ${order.status}"
            )
        }
    }

    private fun transitionFromInProgress(order: Order, event: OrderEvent, actor: CurrentUser): OrderTransitionResult {
        return when (event) {
            OrderEvent.COMPLETE -> {
                val assignedLoaderId = order.acceptedByUserId ?: return OrderTransitionResult.Failure(
                    "Order has no assigned loader"
                )
                if (actor.role == Role.LOADER && actor.id == assignedLoaderId) {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.COMPLETED))
                } else {
                    OrderTransitionResult.Failure("Only assigned loader can complete order")
                }
            }

            OrderEvent.CANCEL -> {
                val assignedLoaderId = order.acceptedByUserId
                val canCancel = actor.id == order.createdByUserId ||
                    (actor.role == Role.LOADER && assignedLoaderId != null && actor.id == assignedLoaderId)
                if (canCancel) {
                    OrderTransitionResult.Success(order.copy(status = OrderStatus.CANCELED))
                } else {
                    OrderTransitionResult.Failure("Only creator or assigned loader can cancel in-progress order")
                }
            }

            OrderEvent.ACCEPT -> OrderTransitionResult.Failure(
                "Cannot accept order from ${order.status}"
            )

            OrderEvent.EXPIRE -> OrderTransitionResult.Failure(
                "Cannot expire order from ${order.status}"
            )
        }
    }
}
