package com.example.cameracontrol

import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf

object AppLogger {
    val logs = mutableStateListOf<String>()
    @Volatile var enableUiLogs: Boolean = false

    fun log(msg: String) {
        if (!BuildConfig.DEBUG) return

        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val formatted = "$time: $msg"
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
        Log.d("AppLogger", msg)
    }
}
