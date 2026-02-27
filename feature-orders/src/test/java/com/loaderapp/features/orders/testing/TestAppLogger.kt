package com.loaderapp.features.orders.testing

import com.loaderapp.core.logging.AppLogger

class TestAppLogger : AppLogger {
    data class LogEvent(
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
    )

    data class BreadcrumbEvent(
        val category: String,
        val message: String,
        val data: Map<String, String>,
    )

    val logs = mutableListOf<LogEvent>()
    val breadcrumbs = mutableListOf<BreadcrumbEvent>()
    val capturedExceptions = mutableListOf<Pair<Throwable, LogEvent>>()

    override fun d(tag: String, message: String) {
        logs += LogEvent(level = "d", tag = tag, message = message)
    }

    override fun i(tag: String, message: String) {
        logs += LogEvent(level = "i", tag = tag, message = message)
    }

    override fun w(tag: String, message: String) {
        logs += LogEvent(level = "w", tag = tag, message = message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        logs += LogEvent(level = "e", tag = tag, message = message, throwable = throwable)
    }

    override fun breadcrumb(category: String, message: String, data: Map<String, String>) {
        breadcrumbs += BreadcrumbEvent(category = category, message = message, data = data)
    }

    override fun captureException(throwable: Throwable, tag: String, message: String) {
        val event = LogEvent(level = "exception", tag = tag, message = message, throwable = throwable)
        logs += event
        capturedExceptions += throwable to event
    }
}
