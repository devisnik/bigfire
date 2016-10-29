package de.devisnik.android.bigmouth

import android.util.Log

class Logger
@SuppressWarnings("unchecked")
constructor(clazz: Class<*>) {
    private val itsTag: String

    init {
        itsTag = clazz.simpleName
    }

    fun d(msg: String): Int {
        if (ENABLED) return Log.d(itsTag, msg)
        return 0
    }

    fun d(msg: String, tr: Throwable): Int {
        if (ENABLED) return Log.d(itsTag, msg, tr)
        return 0
    }

    fun i(msg: String): Int {
        if (ENABLED) return Log.i(itsTag, msg)
        return 0
    }

    fun i(msg: String, tr: Throwable): Int {
        if (ENABLED) return Log.i(itsTag, msg, tr)
        return 0
    }

    fun w(msg: String): Int {
        if (ENABLED) return Log.w(itsTag, msg)
        return 0
    }

    fun w(msg: String, tr: Throwable): Int {
        if (ENABLED) return Log.w(itsTag, msg, tr)
        return 0
    }

    fun e(msg: String): Int {
        if (ENABLED) return Log.e(itsTag, msg)
        return 0
    }

    fun e(msg: String, tr: Throwable): Int {
        if (ENABLED) return Log.e(itsTag, msg, tr)
        return 0
    }

    companion object {

        private val ENABLED = false
    }
}
