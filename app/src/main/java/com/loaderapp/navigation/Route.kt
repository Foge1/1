package com.loaderapp.navigation

sealed class Route(
    val route: String,
) {
    object Splash : Route("splash")

    object Auth : Route("auth")

    object Main : Route("main")

    object Home : Route("home")

    object History : Route("history")

    object Responses : Route("responses")

    object Rating : Route("rating")

    object Profile : Route("profile")

    object Settings : Route("settings")

    object CreateOrder : Route("create_order")

    object Chat : Route("chat/{orderId}") {
        fun createRoute(orderId: Long) = "chat/$orderId"
    }
}

object NavArgs {
    const val ORDER_ID = "orderId"
}
