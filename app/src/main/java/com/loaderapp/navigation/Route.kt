package com.loaderapp.navigation

/**
 * Sealed class для всех корневых роутов навигации.
 */
sealed class Route(val route: String) {

    /** Сплэш-экран */
    object Splash : Route("splash")

    /** Экран выбора роли */
    object Auth : Route("auth")

    /**
     * Главный экран с Bottom Navigation.
     * Содержит userId и isDispatcher — нужны для инициализации вложенных вкладок.
     */
    object Main : Route("main/{${NavArgs.USER_ID}}/{${NavArgs.IS_DISPATCHER}}") {
        fun createRoute(userId: Long, isDispatcher: Boolean) =
            "main/$userId/$isDispatcher"
    }

    /**
     * Детали заказа (открывается поверх Main).
     */
    object OrderDetail : Route("order/{${NavArgs.ORDER_ID}}?${NavArgs.IS_DISPATCHER}={${NavArgs.IS_DISPATCHER}}") {
        fun createRoute(orderId: Long, isDispatcher: Boolean) =
            "order/$orderId?${NavArgs.IS_DISPATCHER}=$isDispatcher"
    }

    // ── Оставлены для обратной совместимости ────────────────────────────────
    @Deprecated("Используй Route.Main", ReplaceWith("Route.Main"))
    object Dispatcher : Route("dispatcher/{${NavArgs.USER_ID}}") {
        fun createRoute(userId: Long) = "dispatcher/$userId"
    }

    @Deprecated("Используй Route.Main", ReplaceWith("Route.Main"))
    object Loader : Route("loader/{${NavArgs.USER_ID}}") {
        fun createRoute(userId: Long) = "loader/$userId"
    }
}

object NavArgs {
    const val USER_ID = "userId"
    const val ORDER_ID = "orderId"
    const val IS_DISPATCHER = "isDispatcher"
}
