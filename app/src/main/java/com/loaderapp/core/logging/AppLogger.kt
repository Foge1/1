package com.loaderapp.core.logging

/**
 * Minimal application-wide logging contract.
 */
interface AppLogger {
    fun d(tag: String, message: String)
}
