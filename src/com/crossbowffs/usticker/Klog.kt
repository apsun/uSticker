package com.crossbowffs.usticker

import android.util.Log

/**
 * Simple unified Android logger, Kotlin version.
 * Works just like the Log class, but without the tag parameter.
 */
object Klog {
    private const val LOG_TAG = BuildConfig.LOG_TAG
    private const val LOG_LEVEL = BuildConfig.LOG_LEVEL

    private fun log(priority: Int, msg: String, e: Throwable? = null) {
        if (priority < LOG_LEVEL) {
            return
        }

        var message = msg
        if (e != null) {
            message += '\n' + Log.getStackTraceString(e)
        }

        Log.println(priority, LOG_TAG, message)
    }

    fun v(message: String, e: Throwable? = null) {
        log(Log.VERBOSE, message, e)
    }

    fun d(message: String, e: Throwable? = null) {
        log(Log.DEBUG, message, e)
    }

    fun i(message: String, e: Throwable? = null) {
        log(Log.INFO, message, e)
    }

    fun w(message: String, e: Throwable? = null) {
        log(Log.WARN, message, e)
    }

    fun e(message: String, e: Throwable? = null) {
        log(Log.ERROR, message, e)
    }
}
