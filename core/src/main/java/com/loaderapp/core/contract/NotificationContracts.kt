package com.loaderapp.core.contract

interface NotificationsRepository {
    suspend fun registerPushToken(token: String)
    suspend fun unregisterPushToken()
    suspend fun acknowledgeHandled(notificationId: String)
}

interface PushTokenRepository {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clear()
}
