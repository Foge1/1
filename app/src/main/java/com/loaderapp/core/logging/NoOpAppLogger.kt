package com.loaderapp.core.logging

import javax.inject.Inject

class NoOpAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) = Unit

    override fun w(tag: String, message: String) = Unit

    override fun e(tag: String, message: String, throwable: Throwable?) = Unit

    override fun breadcrumb(category: String, message: String, data: Map<String, String>) = Unit

    override fun captureException(throwable: Throwable, tag: String, message: String) = Unit
}
