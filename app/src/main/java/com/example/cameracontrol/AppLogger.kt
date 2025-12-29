package com.example.cameracontrol

import android.util.Log
import androidx.compose.runtime.mutableStateListOf

object AppLogger {
    val logs = mutableStateListOf<String>()

    fun log(msg: String) {
        if (!BuildConfig.DEBUG) return

        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val formatted = "$time: $msg"
        
        // Add to top
        logs.add(0, formatted)
        
        // Keep limit
        if (logs.size > 100) {
            logs.removeLast()
        }
        
        // Also log to system logcat
        Log.d("AppLogger", msg)
    }
}
