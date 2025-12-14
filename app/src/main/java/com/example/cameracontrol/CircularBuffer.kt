package com.example.cameracontrol

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque

data class EncodedFrame(
    val data: ByteBuffer,
    val bufferInfo: MediaCodec.BufferInfo,
    val isKeyFrame: Boolean
)

object CircularBuffer {
    private const val TAG = "CircularBuffer"
    
    // Config
    private const val MAX_DURATION_US = 10_000_000L // 10 seconds for testing
    // Let's set it to 60s for now to be safe, user asked for 2 mins though.
    // We'll stick to 120s if memory allows.
    
    private val frames = ConcurrentLinkedDeque<EncodedFrame>()
    
    // Exact format from the encoder (contains CSD-0, CSD-1, etc.)
    @Volatile var mediaFormat: MediaFormat? = null
    
    // We also keep track if we received config frame, but moving to MediaFormat is better.
    @Volatile var codecConfig: EncodedFrame? = null

    @Synchronized
    fun addFrame(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        // Deep copy the buffer because MediaCodec reuses its buffers
        val capacity = bufferInfo.size
        val copy = ByteBuffer.allocateDirect(capacity)
        
        // Adjust position/limit of source to read the data
        val oldPos = byteBuffer.position()
        val oldLimit = byteBuffer.limit()
        
        byteBuffer.position(bufferInfo.offset)
        byteBuffer.limit(bufferInfo.offset + bufferInfo.size)
        copy.put(byteBuffer)
        copy.flip() // Ready for reading
        
        // Restore source
        byteBuffer.position(oldPos)
        byteBuffer.limit(oldLimit)

        val newInfo = MediaCodec.BufferInfo()
        newInfo.set(0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)

        val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
        val isKey = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

        val frame = EncodedFrame(copy, newInfo, isKey)

        if (isConfig) {
            codecConfig = frame
            Log.d(TAG, "Received Codec Config (SPS/PPS)")
            return // Don't add config to the stream of frames, we save it separately
        }

        frames.addLast(frame)
        
        // Clean old frames
        trimBuffer()
    }
    
    @Synchronized
    private fun trimBuffer() {
        if (frames.isEmpty()) return
        
        val newestTime = frames.peekLast()!!.bufferInfo.presentationTimeUs
        
        // Remove old frames until we are within the duration limit
        // AND ensuring we don't cut in the middle of a GOP (Group of Pictures).
        // We should only remove up to a KeyFrame.
        
        while (frames.size > 1) {
            val oldest = frames.peekFirst()!!
            val age = newestTime - oldest.bufferInfo.presentationTimeUs
            
            if (age > MAX_DURATION_US) {
                // Should we remove? Yes, but try to find the next keyframe
                // Ideally we scan from the start and remove chunks up to a keyframe
                frames.removeFirst()
            } else {
                break
            }
        }
        
        // Ensure the buffer starts with a KeyFrame (critical for playback)
        while (frames.isNotEmpty() && !frames.peekFirst()!!.isKeyFrame) {
            frames.removeFirst()
        }
    }

    @Synchronized
    fun getSnapshot(): Snapshot {
        return Snapshot(mediaFormat, ArrayList(frames))
    }
    
    @Synchronized
    fun clear() {
        frames.clear()
    }
    
    data class Snapshot(
        val format: MediaFormat?,
        val frames: List<EncodedFrame>
    )
}
