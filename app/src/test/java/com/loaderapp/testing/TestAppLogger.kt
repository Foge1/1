package com.loaderapp.testing

import com.loaderapp.core.logging.AppLogger

class TestAppLogger : AppLogger {
    data class BreadcrumbEvent(
        val category: String,
        val message: String,
        val data: Map<String, String>,
    )

    val breadcrumbs = mutableListOf<BreadcrumbEvent>()

    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) = Unit

    override fun w(tag: String, message: String) = Unit

    override fun e(tag: String, message: String, throwable: Throwable?) = Unit

    override fun breadcrumb(category: String, message: String, data: Map<String, String>) {
        breadcrumbs += BreadcrumbEvent(category = category, message = message, data = data)
    }

    override fun captureException(throwable: Throwable, tag: String, message: String) = Unit
}
