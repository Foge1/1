package com.loaderapp.features.orders.domain

sealed interface OrderActionBlockReason {
    data object OnlyLoaderCanApply : OrderActionBlockReason
    data object OnlyLoaderCanWithdraw : OrderActionBlockReason
    data object OnlyDispatcherCanStart : OrderActionBlockReason
    data object OnlyDispatcherCreatorCanManageStaffing : OrderActionBlockReason
    data object OnlyDispatcherCreatorCanCancel : OrderActionBlockReason
    data object OnlyDispatcherCreatorOrAssignedLoaderCanComplete : OrderActionBlockReason

    data class ActionAllowedOnlyInStatus(val required: OrderStatus) : OrderActionBlockReason
    data class ActionAllowedOnlyInStatuses(val required: Set<OrderStatus>) : OrderActionBlockReason

    data object ActiveAssignmentExists : OrderActionBlockReason
    data object ApplyLimitReached : OrderActionBlockReason
    data object AlreadyApplied : OrderActionBlockReason
    data object AlreadySelected : OrderActionBlockReason
    data object NoActiveApplicationToWithdraw : OrderActionBlockReason
    data class SelectedCountMismatch(val selected: Int, val required: Int) : OrderActionBlockReason
    data object LoaderNotAssignedToOrder : OrderActionBlockReason

    data class TerminalStatus(val status: OrderStatus) : OrderActionBlockReason
    data class UnsupportedEventForStatus(val event: OrderEvent, val status: OrderStatus) : OrderActionBlockReason
}

fun OrderActionBlockReason.toDisplayMessage(): String = when (this) {
    OrderActionBlockReason.OnlyLoaderCanApply -> "Только грузчик может откликнуться на заказ"
    OrderActionBlockReason.OnlyLoaderCanWithdraw -> "Только грузчик может отозвать отклик"
    OrderActionBlockReason.OnlyDispatcherCanStart -> "Только диспетчер может запустить заказ"
    OrderActionBlockReason.OnlyDispatcherCreatorCanManageStaffing -> "Только диспетчер-создатель может управлять отбором"
    OrderActionBlockReason.OnlyDispatcherCreatorCanCancel -> "Только диспетчер-создатель может отменить заказ"
    OrderActionBlockReason.OnlyDispatcherCreatorOrAssignedLoaderCanComplete ->
        "Завершить заказ может только диспетчер-создатель или назначенный грузчик"

    is OrderActionBlockReason.ActionAllowedOnlyInStatus ->
        "Действие доступно только в статусе ${required.name}"

    is OrderActionBlockReason.ActionAllowedOnlyInStatuses ->
        "Действие доступно только в статусах: ${required.joinToString { it.name }}"

    OrderActionBlockReason.ActiveAssignmentExists -> "У вас уже есть активный заказ"
    OrderActionBlockReason.ApplyLimitReached -> "Достигнут лимит активных откликов (3)"
    OrderActionBlockReason.AlreadyApplied -> "Вы уже откликнулись на этот заказ"
    OrderActionBlockReason.AlreadySelected -> "Вы уже выбраны в этот заказ"
    OrderActionBlockReason.NoActiveApplicationToWithdraw -> "У вас нет активного отклика на этот заказ"
    is OrderActionBlockReason.SelectedCountMismatch -> "Выбрано $selected из $required грузчиков"
    OrderActionBlockReason.LoaderNotAssignedToOrder -> "Вы не назначены на этот заказ"
    is OrderActionBlockReason.TerminalStatus -> "Переход невозможен: заказ в терминальном статусе ${status.name}"
    is OrderActionBlockReason.UnsupportedEventForStatus -> "Событие ${event.name} недопустимо для статуса ${status.name}"
}
