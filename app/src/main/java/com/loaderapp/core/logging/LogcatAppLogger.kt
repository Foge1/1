package com.loaderapp.core.logging

import android.util.Log
import javax.inject.Inject

class LogcatAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun breadcrumb(category: String, message: String, data: Map<String, String>) {
        val payload = if (data.isEmpty()) "" else " data=$data"
        Log.d("Breadcrumb/$category", "$message$payload")
    }

    override fun captureException(throwable: Throwable, tag: String, message: String) {
        Log.e(tag, message, throwable)
    }
}
