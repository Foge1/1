package com.loaderapp.presentation.common

import com.loaderapp.R
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.UiText

fun AppError.toUiText(): UiText = when (this) {
    AppError.Network.NoInternet -> UiText.Resource(R.string.error_no_internet)
    AppError.Network.Timeout -> UiText.Resource(R.string.error_timeout)
    AppError.Network.Dns,
    AppError.Network.UnknownHost -> UiText.Resource(R.string.error_unknown_host)

    is AppError.Backend -> {
        when (httpCode) {
            401 -> UiText.Resource(R.string.error_unauthorized)
            403 -> UiText.Resource(R.string.error_forbidden)
            404 -> UiText.Resource(R.string.error_not_found)
            else -> UiText.Dynamic(serverMessage ?: "HTTP $httpCode")
        }
    }

    AppError.Auth.Unauthorized -> UiText.Resource(R.string.error_unauthorized)
    AppError.Auth.Forbidden -> UiText.Resource(R.string.error_forbidden)
    AppError.Auth.SessionExpired -> UiText.Resource(R.string.error_session_expired)

    is AppError.Validation -> {
        val message = message
        if (!message.isNullOrBlank()) UiText.Dynamic(message) else UiText.Resource(R.string.error_validation)
    }

    AppError.NotFound -> UiText.Resource(R.string.error_not_found)

    is AppError.Storage.Db -> UiText.Resource(R.string.error_storage_db)
    is AppError.Storage.Serialization -> UiText.Resource(R.string.error_storage_serialization)

    is AppError.Unknown -> UiText.Resource(R.string.error_unknown)
}
