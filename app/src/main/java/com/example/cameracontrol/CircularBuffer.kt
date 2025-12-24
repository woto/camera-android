package com.example.cameracontrol

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque

data class EncodedFrame(
    val data: ByteBuffer,
    val bufferInfo: MediaCodec.BufferInfo,
    val isKeyFrame: Boolean,
    val type: CircularBuffer.FrameType
)

object CircularBuffer {
    enum class FrameType { VIDEO, AUDIO }
    
    private const val TAG = "CircularBuffer"
    
    // Config
    private const val MAX_DURATION_US = 10_000_000L // 10 seconds
    
    private val frames = ConcurrentLinkedDeque<EncodedFrame>()
    
    // Exact format from the encoder (contains CSD-0, CSD-1, etc.)
    @Volatile var videoFormat: MediaFormat? = null
    @Volatile var audioFormat: MediaFormat? = null
    @Volatile var rotationDegrees: Int = 0
    
    // We also keep track if we received config frame, but moving to MediaFormat is better.
    @Volatile var codecConfig: EncodedFrame? = null

    @Synchronized
    fun addFrame(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, type: FrameType) {
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

        val frame = EncodedFrame(copy, newInfo, isKey, type)

        if (isConfig) {
            // For AAC, config might come here too.
             Log.d(TAG, "Received Codec Config ($type)")
             return 
        }

        frames.addLast(frame)
        
        // Clean old frames
        trimBuffer()
    }
    
    @Synchronized
    private fun trimBuffer() {
        if (frames.isEmpty()) return
        
        val newestTime = frames.peekLast()!!.bufferInfo.presentationTimeUs
        
        while (frames.size > 1) {
            val oldest = frames.peekFirst()!!
            val age = newestTime - oldest.bufferInfo.presentationTimeUs
            
            if (age > MAX_DURATION_US) {
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
        return Snapshot(videoFormat, audioFormat, ArrayList(frames), rotationDegrees)
    }
    
    @Synchronized
    fun clear() {
        frames.clear()
    }
    
    data class Snapshot(
        val videoFormat: MediaFormat?,
        val audioFormat: MediaFormat?,
        val frames: List<EncodedFrame>,
        val rotation: Int
    )
}
