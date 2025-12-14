package com.example.cameracontrol

import android.util.Log
import java.io.File

object BufferManager {
    private const val TAG = "BufferManager"
    private const val MAX_BUFFER_DURATION_MS = 5_000L // 5 seconds
    private val buffer = ArrayDeque<File>()

    // Simplified: assuming each file is roughly the same duration (e.g. 1s)
    private const val ESTIMATED_CHUNK_DURATION_MS = 1000L

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

    private var lastTriggerTime = 0L

    fun triggerUpload() {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 2000) {
            Log.d(TAG, "Trigger debounced (too fast)")
            return
        }
        lastTriggerTime = now

        synchronized(buffer) {
            if (buffer.isEmpty()) return
            
            val filesToUpload = ArrayList(buffer)
            Log.i(TAG, "TRIGGER: Copying ${filesToUpload.size} files to stage...")

            filesToUpload.forEach { sourceFile ->
                if (sourceFile.exists()) {
                    try {
                        // Create staging directory
                        val uploadDir = File(sourceFile.parentFile, "uploads")
                        if (!uploadDir.exists()) uploadDir.mkdirs()

                        // Copy file to staging area with UNIQUE name to avoid race conditions
                        // (e.g. if trigger happens twice quickly)
                        val uniqueName = "staged_${java.util.UUID.randomUUID()}_${sourceFile.name}"
                        val stagedFile = File(uploadDir, uniqueName)
                        sourceFile.copyTo(stagedFile, overwrite = true)
                        
                        Log.i(TAG, "Staged: ${stagedFile.name}")
                        
                        // Upload the STAGED file, but tell server it's the original name
                        NetworkClient.uploadFile(stagedFile, sourceFile.name) {
                            // CLEANUP after upload (or failure)
                            if (stagedFile.exists()) {
                                stagedFile.delete()
                                Log.d(TAG, "Cleaned up staged file: ${stagedFile.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed into stage file", e)
                    }
                }
            }
        }
    }
}
