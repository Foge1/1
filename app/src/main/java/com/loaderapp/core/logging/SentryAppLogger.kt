package com.loaderapp.core.logging

import android.util.Log
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import javax.inject.Inject

class SentryAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
        Sentry.captureMessage("$tag: $message", SentryLevel.WARNING)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        if (throwable != null) {
            captureException(throwable, tag, message)
        } else {
            Sentry.captureMessage("$tag: $message", SentryLevel.ERROR)
        }
    }

    override fun breadcrumb(category: String, message: String, data: Map<String, String>) {
        Sentry.addBreadcrumb(Breadcrumb.info(message).apply {
            this.category = category
            level = SentryLevel.INFO
            data.forEach { (key, value) -> setData(key, value) }
        })
    }

    override fun captureException(throwable: Throwable, tag: String, message: String) {
        Sentry.configureScope { scope ->
            scope.setTag("logger_tag", tag)
            scope.setExtra("logger_message", message)
        }
        Sentry.captureException(throwable)
    }
}
