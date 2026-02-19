package com.loaderapp.navigation

sealed class Route(val route: String) {

    object Splash   : Route("splash")
    object Auth     : Route("auth")
    object Main     : Route("main")

    // ── Вкладки Bottom Nav ───────────────────────────────────────────────────
    object Home     : Route("home")
    object History  : Route("history")
    object Rating   : Route("rating")
    object Profile  : Route("profile")
    object Settings : Route("settings")

    // ── Поверх Main ──────────────────────────────────────────────────────────
    object OrderDetail : Route("order/{orderId}?isDispatcher={isDispatcher}") {
        fun createRoute(orderId: Long, isDispatcher: Boolean) =
            "order/$orderId?isDispatcher=$isDispatcher"
    }
}

object NavArgs {
    const val ORDER_ID      = "orderId"
    const val IS_DISPATCHER = "isDispatcher"
}
