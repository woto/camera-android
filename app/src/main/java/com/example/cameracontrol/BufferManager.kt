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
        Log.d(TAG, "Snapshot: frames=${frames.size} rot=${snapshot.rotation}")
        AppLogger.log("Snapshot: frames=${frames.size} rot=${snapshot.rotation}")
        
        if (frames.isEmpty()) {
            Log.w(TAG, "Buffer is empty, nothing to save.")
            AppLogger.log("Buffer Empty!")
            return
        }
        
        // Fix: Sort frames by presentation time to ensure monotonic writing
        // and find the true minimum timestamp to prevent negative values.
        val sortedFrames = frames.sortedBy { it.bufferInfo.presentationTimeUs }
        val videoFrameCount = sortedFrames.count { it.type == CircularBuffer.FrameType.VIDEO }
        val audioFrameCount = sortedFrames.count { it.type == CircularBuffer.FrameType.AUDIO }
        val hasAudioFrames = audioFrameCount > 0
        Log.d(TAG, "Muxer frames: video=$videoFrameCount audio=$audioFrameCount")
        AppLogger.log("Muxer frames: v=$videoFrameCount a=$audioFrameCount")
        
        val firstTimeUs = sortedFrames.first().bufferInfo.presentationTimeUs
        val lastTimeUs = sortedFrames.last().bufferInfo.presentationTimeUs
        
        val startTime = firstTimeUs / 1000 // to ms
        val endTime = lastTimeUs / 1000 // to ms
        val systemTime = System.currentTimeMillis()
        Log.d(TAG, "PTS: first=$firstTimeUs last=$lastTimeUs startMs=$startTime endMs=$endTime")
        AppLogger.log("PTS: first=$firstTimeUs last=$lastTimeUs")
        
        // Approximate the real start/end time based on system clock
        val endEpoch = systemTime
        val startEpoch = endEpoch - (endTime - startTime)

        // 2. Create Output File: [device_id]-[start]-[end].mp4
        val fileName = "${deviceId}-${startEpoch}-${endEpoch}.mp4"
        val outputFile = File(outputDir, fileName)
        Log.d(TAG, "Muxer output: ${outputFile.absolutePath}")
        AppLogger.log("Muxer output: ${outputFile.name}")

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
                Log.d(TAG, "Video track added. rotation=${snapshot.rotation} format=$videoFormat")
                AppLogger.log("Video track added rot=${snapshot.rotation}")
            } else {
                Log.e(TAG, "Missing Video Format!")
                return
            }
            
            if (audioFormat != null && hasAudioFrames) {
                audioTrackIndex = muxer.addTrack(audioFormat)
                AppLogger.log("Muxing: Vid+Aud")
                Log.d(TAG, "Audio track added. format=$audioFormat")
            } else {
                Log.w(TAG, "Missing Audio Format - Muxing Video Only")
                AppLogger.log("No Audio Fmt!")
            }
            
            muxer.start()
            Log.d(TAG, "Muxer started (v=$videoTrackIndex a=$audioTrackIndex)")
            AppLogger.log("Muxer started")
            
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
            Log.d(TAG, "Muxer stopped and released")
            AppLogger.log("Muxer stopped")
            
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
            try {
                muxer?.release()
            } catch (_: Exception) { }
        }
    }
}
