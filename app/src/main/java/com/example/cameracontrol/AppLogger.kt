package com.example.cameracontrol

import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf

object AppLogger {
    private const val APP_TAG = "CameraControl"
    val logs = mutableStateListOf<String>()
    @Volatile var enableUiLogs: Boolean = false

    fun log(msg: String) {
        log("", msg)
    }

    fun log(tag: String, msg: String) {
        logInternal(tag, msg, Level.DEBUG, null, debugOnly = true)
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        logInternal(tag, msg, Level.WARN, tr, debugOnly = false)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        logInternal(tag, msg, Level.ERROR, tr, debugOnly = false)
    }

    private fun logInternal(
        tag: String,
        msg: String,
        level: Level,
        tr: Throwable?,
        debugOnly: Boolean
    ) {
        if (debugOnly && !BuildConfig.DEBUG) return

        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val tagPrefix = if (tag.isNotBlank()) "[$tag] " else ""
        val formatted = "$time: $tagPrefix$msg"
        if (enableUiLogs) {
            val appendLog = {
                logs.add(0, formatted)
                if (logs.size > 100) {
                    logs.removeLast()
                }
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                appendLog()
            } else {
                Handler(Looper.getMainLooper()).post { appendLog() }
            }
        }

        // Also log to system logcat
        when (level) {
            Level.DEBUG -> Log.d(APP_TAG, "$tagPrefix$msg")
            Level.WARN -> if (tr != null) {
                Log.w(APP_TAG, "$tagPrefix$msg", tr)
            } else {
                Log.w(APP_TAG, "$tagPrefix$msg")
            }
            Level.ERROR -> if (tr != null) {
                Log.e(APP_TAG, "$tagPrefix$msg", tr)
            } else {
                Log.e(APP_TAG, "$tagPrefix$msg")
            }
        }
    }

    private enum class Level {
        DEBUG,
        WARN,
        ERROR
    }
}
