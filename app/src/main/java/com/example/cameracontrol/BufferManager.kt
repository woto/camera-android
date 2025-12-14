package com.example.cameracontrol

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File

object BufferManager {
    private const val TAG = "BufferManager"
    private var lastTriggerTime = 0L
    private lateinit var outputDir: File
    private lateinit var deviceId: String

    fun initialize(context: android.content.Context) {
        outputDir = context.cacheDir
        deviceId = DeviceIdentity.getDeviceId(context)
    }

    fun triggerUpload() {
        if (!::outputDir.isInitialized) {
            Log.e(TAG, "BufferManager not initialized!")
            return
        }
        
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 3000) {
            Log.d(TAG, "Trigger debounced (too fast)")
            return
        }
        lastTriggerTime = now
        
        AppLogger.log("Processing Capture...")

        // 1. Get Snapshot from RAM
        val snapshot = CircularBuffer.getSnapshot()
        val frames = snapshot.frames
        
        if (frames.isEmpty()) {
            Log.w(TAG, "Buffer is empty, nothing to save.")
            AppLogger.log("Buffer Empty!")
            return
        }
        
        val startTime = frames.first().bufferInfo.presentationTimeUs / 1000 // to ms
        val endTime = frames.last().bufferInfo.presentationTimeUs / 1000 // to ms
        val systemTime = System.currentTimeMillis()
        
        // Approximate the real start/end time based on system clock
        // (This is an approximation, but better than raw uptimeUs)
        val endEpoch = systemTime
        val startEpoch = endEpoch - (endTime - startTime)

        // 2. Create Output File: [device_id]-[start]-[end].mp4
        val fileName = "${deviceId}-${startEpoch}-${endEpoch}.mp4"
        val outputFile = File(outputDir, fileName)

        try {
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // 3. Configure Track using the captured format
            val videoFormat = snapshot.format
            
            if (videoFormat != null) {
                // We trust the encoder's format which includes CSD
                val trackIndex = muxer.addTrack(videoFormat)
                
                muxer.setOrientationHint(snapshot.rotation)
                
                muxer.start()
                
                // 4. Write Frames
                val firstTimeUs = frames.first().bufferInfo.presentationTimeUs
                
                for (frame in frames) {
                    val info = MediaCodec.BufferInfo()
                    info.set(
                        frame.bufferInfo.offset,
                        frame.bufferInfo.size,
                        frame.bufferInfo.presentationTimeUs - firstTimeUs,
                        frame.bufferInfo.flags
                    )
                    
                    muxer.writeSampleData(trackIndex, frame.data, info)
                }
                
                muxer.stop()
                muxer.release()
                
                Log.d(TAG, "Saved MP4: ${outputFile.length()} bytes")
                AppLogger.log("Saved MP4: ${outputFile.length() / 1024} KB")

                // 5. Upload
                NetworkClient.uploadFile(outputFile, outputFile.name) {
                    if (outputFile.exists()) outputFile.delete()
                    AppLogger.log("Upload & Cleanup Done")
                }
            } else {
                 Log.e(TAG, "Missing Codec Format! Cannot mux.")
                 AppLogger.log("Err: Missing Format")
                 return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Muxing failed", e)
            AppLogger.log("Mux Error: ${e.message}")
        }
    }
}

