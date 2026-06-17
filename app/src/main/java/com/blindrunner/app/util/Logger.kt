package com.blindrunner.app.util

import android.util.Log

/**
 * 统一日志工具 — release 版本自动关闭 debug/verbose 日志。
 * 用法：Logger.d("Tag", "message")  替代 Log.d("Tag", "message")
 */
object Logger {
    var isDebug = true  // release 时设为 false

    fun d(tag: String, msg: String) { if (isDebug) Log.d(tag, msg) }
    fun i(tag: String, msg: String) { Log.i(tag, msg) }
    fun w(tag: String, msg: String) { Log.w(tag, msg) }
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
    }
}

/** Kotlin 扩展，方便链式调用 */
fun Any.logd(msg: String) = Logger.d(this::class.java.simpleName, msg)
fun Any.loge(msg: String, tr: Throwable? = null) = Logger.e(this::class.java.simpleName, msg, tr)
