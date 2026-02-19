package com.loaderapp.navigation

sealed class Route(val route: String) {

    object Splash : Route("splash")

    object Auth : Route("auth")

    /** Единый главный экран для всех ролей */
    object Main : Route("main")

    // Вложенные роуты внутри Main (bottom nav)
    object Home     : Route("home")
    object Profile  : Route("profile")
    object Settings : Route("settings")

    // Детали заказа — поверх Main
    object OrderDetail : Route("order/{orderId}?isDispatcher={isDispatcher}") {
        fun createRoute(orderId: Long, isDispatcher: Boolean) =
            "order/$orderId?isDispatcher=$isDispatcher"
    }
}

object NavArgs {
    const val ORDER_ID      = "orderId"
    const val IS_DISPATCHER = "isDispatcher"
}
