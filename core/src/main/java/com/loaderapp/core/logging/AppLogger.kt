package com.loaderapp.core.logging

/**
 * Minimal application-wide logging contract.
 */
interface AppLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun breadcrumb(category: String, message: String, data: Map<String, String> = emptyMap())
    fun captureException(throwable: Throwable, tag: String, message: String)
}
