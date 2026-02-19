package com.loaderapp.navigation

/**
 * Sealed class для всех роутов навигации в приложении
 * Каждый экран имеет свой route с параметрами
 */
sealed class Route(val route: String) {
    
    /**
     * Splash экран (стартовый)
     */
    object Splash : Route("splash")
    
    /**
     * Экран выбора роли (аутентификация)
     */
    object Auth : Route("auth")
    
    /**
     * Экран диспетчера
     * @param userId ID текущего пользователя
     */
    object Dispatcher : Route("dispatcher/{userId}") {
        fun createRoute(userId: Long) = "dispatcher/$userId"
    }
    
    /**
     * Экран грузчика
     * @param userId ID текущего пользователя
     */
    object Loader : Route("loader/{userId}") {
        fun createRoute(userId: Long) = "loader/$userId"
    }
    
    /**
     * Детали заказа
     * @param orderId ID заказа
     * @param isDispatcher true если открывает диспетчер
     */
    object OrderDetail : Route("order/{orderId}?isDispatcher={isDispatcher}") {
        fun createRoute(orderId: Long, isDispatcher: Boolean) = 
            "order/$orderId?isDispatcher=$isDispatcher"
    }
}

/**
 * Аргументы навигации
 */
object NavArgs {
    const val USER_ID = "userId"
    const val ORDER_ID = "orderId"
    const val IS_DISPATCHER = "isDispatcher"
}
