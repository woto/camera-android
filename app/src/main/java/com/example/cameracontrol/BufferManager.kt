package com.example.cameracontrol

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

object BufferManager {
    private const val TAG = "BufferManager"
    private var lastTriggerTime = 0L
    private lateinit var outputDir: File
    private lateinit var deviceId: String
    private var rotationProvider: (() -> Int)? = null
    private var onClipSaved: (() -> Unit)? = null

    fun initialize(context: android.content.Context) {
        outputDir = context.cacheDir
        deviceId = DeviceIdentity.getDeviceId(context)
    }

    fun setRotationProvider(provider: () -> Int) {
        rotationProvider = provider
    }

    fun setOnClipSaved(callback: (() -> Unit)?) {
        onClipSaved = callback
    }

    fun triggerUpload(triggerTimestamp: String? = null) {
        if (!::outputDir.isInitialized) {
            AppLogger.e(TAG, "BufferManager not initialized!")
            return
        }

        AppLogger.log(TAG, "triggerUpload() ts=${triggerTimestamp ?: "null"}")
        rotationProvider?.invoke()?.let { latest ->
            CircularBuffer.rotationDegrees = latest
        }
        
        val now = System.currentTimeMillis()
        val deltaMs = now - lastTriggerTime
        if (deltaMs < 3000) {
            AppLogger.log(TAG, "Trigger debounced (too fast): ${deltaMs}ms since last")
            return
        }
        lastTriggerTime = now
        
        AppLogger.log(TAG, "Processing Capture...")

        // 1. Get Snapshot from RAM
        val snapshot = CircularBuffer.getSnapshot()
        val frames = snapshot.frames
        // AppLogger.log(TAG, "Snapshot: frames=${frames.size} rot=${snapshot.rotation}")
        
        if (frames.isEmpty()) {
            AppLogger.w(TAG, "Buffer is empty, nothing to save.")
            return
        }
        
        // Fix: Sort frames by presentation time to ensure monotonic writing
        // and find the true minimum timestamp to prevent negative values.
        val sortedFrames = frames.sortedBy { it.bufferInfo.presentationTimeUs }
        val videoFrameCount = sortedFrames.count { it.type == CircularBuffer.FrameType.VIDEO }
        val audioFrameCount = sortedFrames.count { it.type == CircularBuffer.FrameType.AUDIO }
        val hasAudioFrames = audioFrameCount > 0
        AppLogger.log(TAG, "Muxer frames: video=$videoFrameCount audio=$audioFrameCount")
        
        val firstTimeUs = sortedFrames.first().bufferInfo.presentationTimeUs
        val lastTimeUs = sortedFrames.last().bufferInfo.presentationTimeUs
        
        val startTime = firstTimeUs / 1000 // to ms
        val endTime = lastTimeUs / 1000 // to ms
        val systemTime = System.currentTimeMillis()
        AppLogger.log(TAG, "PTS: first=$firstTimeUs last=$lastTimeUs startMs=$startTime endMs=$endTime")
        
        // Approximate the real start/end time based on system clock
        val endEpoch = systemTime
        val startEpoch = endEpoch - (endTime - startTime)

        // 2. Create Output File: [device_id]-[start]-[end].mp4
        val fileName = "${deviceId}-${startEpoch}-${endEpoch}.mp4"
        val outputFile = File(outputDir, fileName)
        AppLogger.log(TAG, "Muxer output: ${outputFile.absolutePath}")

        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // 3. Configure Tracks
            val videoFormat = snapshot.videoFormat
            val audioFormat = snapshot.audioFormat
            
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            
            if (videoFormat != null) {
                videoTrackIndex = muxer.addTrack(videoFormat)
                muxer.setOrientationHint(snapshot.rotation)
                // AppLogger.log(TAG, "Video track added. rotation=${snapshot.rotation} format=$videoFormat")
            } else {
                AppLogger.e(TAG, "Missing Video Format!")
                return
            }
            
            if (audioFormat != null && hasAudioFrames) {
                audioTrackIndex = muxer.addTrack(audioFormat)
                AppLogger.log(TAG, "Muxing: Vid+Aud")
                AppLogger.log(TAG, "Audio track added. format=$audioFormat")
            } else {
                AppLogger.w(TAG, "Missing Audio Format - Muxing Video Only")
            }
            
            muxer.start()
            AppLogger.log(TAG, "Muxer started (v=$videoTrackIndex a=$audioTrackIndex)")
            
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
            
            try {
                muxer.stop()
            } finally {
                muxer.release()
            }
            AppLogger.log(TAG, "Muxer stopped and released")
            onClipSaved?.invoke()
            
        AppLogger.log(TAG, "Saved MP4: ${outputFile.length()} bytes")

        // 5. Upload
        val currentRoom = NetworkClient.getCurrentRoomId()
        AppLogger.log(TAG, "Upload start: ${outputFile.name} room=${currentRoom ?: "null"}")
        NetworkClient.uploadFile(outputFile, outputFile.name, triggerTimestamp, currentRoom) {
            if (outputFile.exists()) outputFile.delete()
            AppLogger.log(TAG, "Upload & Cleanup Done")
        }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Muxing failed", e)
            try {
                muxer?.release()
            } catch (_: Exception) { }
        }
    }
}
