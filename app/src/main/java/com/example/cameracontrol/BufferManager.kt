package com.example.cameracontrol

import android.util.Log
import java.io.File

object BufferManager {
    private const val TAG = "BufferManager"
    private const val MAX_BUFFER_DURATION_MS = 20_000L // 20 seconds
    private val buffer = ArrayDeque<File>()

    // Simplified: assuming each file is roughly the same duration (e.g. 5s)
    // In a real app complexity, we would parse video metadata for exact duration.
    private const val ESTIMATED_CHUNK_DURATION_MS = 5000L

    fun addFile(file: File) {
        synchronized(buffer) {
            buffer.addLast(file)
            Log.d(TAG, "Added file: ${file.name}. Buffer size: ${buffer.size}")
            cleanOldFiles()
        }
    }

    private fun cleanOldFiles() {
        // Calculate total duration roughly
        while (buffer.size * ESTIMATED_CHUNK_DURATION_MS > MAX_BUFFER_DURATION_MS) {
            val oldFile = buffer.removeFirst()
            if (oldFile.exists()) {
                val deleted = oldFile.delete()
                Log.d(TAG, "Deleted old file: ${oldFile.name}, success: $deleted")
            }
        }
    }

    fun triggerUpload() {
        synchronized(buffer) {
            if (buffer.isEmpty()) return
            
            Log.i(TAG, "TRIGGER RECEIVED! Uploading last ${buffer.size} segments.")
            val filesToUpload = ArrayList(buffer)
            
            // In a real app, we would copy these files to a separate directory 
            // so the recording loop doesn't delete them while uploading.
            // For this prototype, we just log them.
            
            filesToUpload.forEach { file ->
                Log.i(TAG, "Uploading file: ${file.absolutePath} (${file.length()} bytes)")
            }
            
            // Simulate Upload Network Request
            // NetworkManager.upload(filesToUpload)
        }
    }
}
