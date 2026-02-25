package com.loaderapp.core.common

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Typed application error model used across data/domain/presentation layers.
 */
sealed interface AppError {
    sealed interface Network : AppError {
        data object Timeout : Network
        data object NoInternet : Network
        data object Dns : Network
        data object UnknownHost : Network
    }

    data class Backend(
        val httpCode: Int,
        val serverMessage: String? = null,
        val errorCode: String? = null
    ) : AppError

    sealed interface Auth : AppError {
        data object Unauthorized : Auth
        data object Forbidden : Auth
        data object SessionExpired : Auth
    }

    data class Validation(
        val message: String? = null,
        val fieldErrors: Map<String, String>? = null
    ) : AppError

    data object NotFound : AppError

    sealed interface Storage : AppError {
        data class Db(val cause: Throwable? = null) : Storage
        data class Serialization(val cause: Throwable? = null) : Storage
    }

    data class Unknown(val cause: Throwable? = null) : AppError
}

/** Maps any [Throwable] to a typed [AppError]. */
fun Throwable.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Network.Timeout
    is UnknownHostException -> AppError.Network.UnknownHost
    is ConnectException -> AppError.Network.NoInternet
    is IOException -> {
        val normalized = message.orEmpty().lowercase()
        if (normalized.contains("dns")) AppError.Network.Dns else AppError.Network.NoInternet
    }
    else -> AppError.Unknown(this)
}
