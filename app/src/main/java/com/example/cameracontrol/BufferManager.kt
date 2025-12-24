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
    private var rotationProvider: (() -> Int)? = null

    fun initialize(context: android.content.Context) {
        outputDir = context.cacheDir
        deviceId = DeviceIdentity.getDeviceId(context)
    }

    fun setRotationProvider(provider: () -> Int) {
        rotationProvider = provider
    }

    fun triggerUpload(triggerTimestamp: String? = null) {
        if (!::outputDir.isInitialized) {
            Log.e(TAG, "BufferManager not initialized!")
            return
        }

        rotationProvider?.invoke()?.let { latest ->
            CircularBuffer.rotationDegrees = latest
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
        
        // Fix: Sort frames by presentation time to ensure monotonic writing
        // and find the true minimum timestamp to prevent negative values.
        val sortedFrames = frames.sortedBy { it.bufferInfo.presentationTimeUs }
        
        val firstTimeUs = sortedFrames.first().bufferInfo.presentationTimeUs
        val lastTimeUs = sortedFrames.last().bufferInfo.presentationTimeUs
        
        val startTime = firstTimeUs / 1000 // to ms
        val endTime = lastTimeUs / 1000 // to ms
        val systemTime = System.currentTimeMillis()
        
        // Approximate the real start/end time based on system clock
        val endEpoch = systemTime
        val startEpoch = endEpoch - (endTime - startTime)

        // 2. Create Output File: [device_id]-[start]-[end].mp4
        val fileName = "${deviceId}-${startEpoch}-${endEpoch}.mp4"
        val outputFile = File(outputDir, fileName)

        try {
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // 3. Configure Tracks
            val videoFormat = snapshot.videoFormat
            val audioFormat = snapshot.audioFormat
            
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            
            if (videoFormat != null) {
                videoTrackIndex = muxer.addTrack(videoFormat)
                muxer.setOrientationHint(snapshot.rotation)
            } else {
                Log.e(TAG, "Missing Video Format!")
                return
            }
            
            if (audioFormat != null) {
                audioTrackIndex = muxer.addTrack(audioFormat)
                AppLogger.log("Muxing: Vid+Aud")
            } else {
                Log.w(TAG, "Missing Audio Format - Muxing Video Only")
                AppLogger.log("No Audio Fmt!")
            }
            
            muxer.start()
            
            // 4. Write Frames
            if (sortedFrames.isNotEmpty()) {
                for (frame in sortedFrames) {
                    val trackIndex = if (frame.type == CircularBuffer.FrameType.VIDEO) videoTrackIndex else audioTrackIndex
                    
                    if (trackIndex >= 0) {
                        val info = MediaCodec.BufferInfo()
                        val pts = frame.bufferInfo.presentationTimeUs
                        
                        // Fix: Ensure timestamp is never negative relative to start
                        val relativePts = maxOf(0L, pts - firstTimeUs)
                        
                        info.set(
                            frame.bufferInfo.offset,
                            frame.bufferInfo.size,
                            relativePts,
                            frame.bufferInfo.flags
                        )
                        
                        muxer.writeSampleData(trackIndex, frame.data, info)
                    }
                }
            }
            
            muxer.stop()
            muxer.release()
            
            Log.d(TAG, "Saved MP4: ${outputFile.length()} bytes")
            AppLogger.log("Saved MP4: ${outputFile.length() / 1024} KB")

            // 5. Upload
            val currentRoom = NetworkClient.getCurrentRoomId()
            NetworkClient.uploadFile(outputFile, outputFile.name, triggerTimestamp, currentRoom) {
                if (outputFile.exists()) outputFile.delete()
                AppLogger.log("Upload & Cleanup Done")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Muxing failed", e)
            AppLogger.log("Mux Error: ${e.message}")
        }
    }
}
