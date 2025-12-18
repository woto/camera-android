package com.example.cameracontrol

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var encodingPreview: Preview? = null // Used to feed the encoder
    private var audioCodec: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var isAudioRecording = false
    private var audioSampleRate = DEFAULT_AUDIO_SAMPLE_RATE
    private var camera: Camera? = null
    
    // Missing properties
    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var isRecording = false
    // Fix: Use 3 threads (Video Loop, Audio Loop, +1 for Surface Callback/Spare)
    private val executor: ExecutorService = Executors.newFixedThreadPool(3)
    
    companion object {
        private const val TAG = "VideoRecorder"
        // 720p is good for performance/quality balance
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        private const val BIT_RATE = 2_500_000 // 2.5 Mbps
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1 // Keyframe every 1 second for easier cutting
        
        // Audio Config
        private const val DEFAULT_AUDIO_SAMPLE_RATE = 48000
        private const val AUDIO_BIT_RATE = 64000
        private const val AUDIO_CHANNEL_COUNT = 1 // Mono
        private val PREFERRED_SAMPLE_RATES = listOf(48000, 44100, 32000, 16000)
    }

    fun startCamera(surfaceProvider: Preview.SurfaceProvider) {
        AppLogger.log("startCamera() called")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            AppLogger.log("Camera Provider Ready")
            cameraProvider = cameraProviderFuture.get()
            
            // 1. UI Preview (Viewfinder)
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(surfaceProvider)
                }

            // 2. Prepare Encoder & Input Surface
            setupMediaCodec()       // Video
            setupAudioMediaCodec()  // Audio
            
            // 3. Recorder Preview (Feeds the Encoder)
            encodingPreview = Preview.Builder()
                .setTargetName("EncodingPreview")
                .build()

            // We need to bridge the Encoder's Surface to this Preview
            // Once the codec is configured, inputSurface is ready.
            encodingPreview?.setSurfaceProvider { request ->
                // Capture rotation
                val cameraInfo = cameraProvider?.availableCameraInfos?.firstOrNull {
                     CameraSelector.DEFAULT_BACK_CAMERA.filter(listOf(it)).isNotEmpty()
                }
                val rotation = cameraInfo?.getSensorRotationDegrees() ?: 90
                CircularBuffer.rotationDegrees = rotation
                Log.d(TAG, "Camera Rotation Set: $rotation")

                if (inputSurface != null) {
                    request.provideSurface(inputSurface!!, executor) { result -> 
                        // Surface release callback
                        Log.d(TAG, "Encoding surface request result: ${result.resultCode}")
                    }
                } else {
                    request.willNotProvideSurface()
                }
            }

            try {
                cameraProvider?.unbindAll()
                // Bind both the UI preview and the Encoding preview
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    encodingPreview
                )
                
                isRecording = true
                startEncodingLoop()         // Video Loop
                AppLogger.log("Starting Audio Loop...")
                startAudioEncodingLoop()    // Audio Loop
                
                Log.d(TAG, "Camera and Recording started seamlessly")
                
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                AppLogger.log("Camera Error: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupMediaCodec() {
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            // Create the input surface *after* configure and *before* start
            inputSurface = mediaCodec?.createInputSurface()
            
            mediaCodec?.start()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create MediaCodec", e)
            AppLogger.log("Codec Error: ${e.message}")
        }
    }
    
    private fun setupAudioMediaCodec() {
        AppLogger.log("Setting up Audio Codec...")
        try {
            audioSampleRate = resolveSampleRate()
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioSampleRate, AUDIO_CHANNEL_COUNT)
            format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)

            audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioCodec?.start()

            // Setup AudioRecord
            val minBufferSize = AudioRecord.getMinBufferSize(
                audioSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (minBufferSize <= 0) {
                Log.e(TAG, "Unsupported audio config. Min buffer=$minBufferSize at rate=$audioSampleRate")
                AppLogger.log("Audio cfg not supported")
                releaseAudioCodecSafely()
                return
            }
            
            val perm = android.Manifest.permission.RECORD_AUDIO
            val check = ContextCompat.checkSelfPermission(context, perm)
            Log.d(TAG, "Checking Perm: $perm on ${context.packageName}. Result: $check (Expected: ${android.content.pm.PackageManager.PERMISSION_GRANTED})")

            // Fix: Increase buffer size to prevent underruns while keeping latency reasonable
            val bufferSize = minBufferSize * 6
            val audioSource = pickAudioSource()

            if (check == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                audioRecord = AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .setSampleRate(audioSampleRate)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize!")
                    AppLogger.log("AudioRecord Init Failed")
                    releaseAudioCodecSafely()
                    audioRecord = null
                } else {
                    Log.d(TAG, "AudioRecord initialized (src=$audioSource, rate=$audioSampleRate, buf=$bufferSize)")
                    AppLogger.log("Mic Init OK (src=$audioSource, rate=$audioSampleRate, buf=$bufferSize)")
                }
            } else {
                Log.e(TAG, "RECORD_AUDIO Permission NOT GRANTED")
                AppLogger.log("No Audio Perm")
                releaseAudioCodecSafely()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioCodec", e)
            AppLogger.log("Audio Codec Err: ${e.message}")
            releaseAudioCodecSafely()
        }
    }

    private fun startEncodingLoop() {
        executor.submit {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isRecording && mediaCodec != null) {
                try {
                    val encoderStatus = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1

                    if (encoderStatus >= 0) {
                        val encodedData = mediaCodec?.getOutputBuffer(encoderStatus)
                        if (encodedData != null) {
                            if (bufferInfo.size > 0) {
                                CircularBuffer.addFrame(encodedData, bufferInfo, CircularBuffer.FrameType.VIDEO)
                            }
                            mediaCodec?.releaseOutputBuffer(encoderStatus, false)
                        }
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // This usually contains the CSD-0/CSD-1 (SPS/PPS) which are critical
                        val newFormat = mediaCodec?.outputFormat
                        Log.d(TAG, "Output format changed: $newFormat")
                        if (newFormat != null) {
                            CircularBuffer.videoFormat = newFormat
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in encoding loop", e)
                }
            }
        }
    }

    private fun resolveSampleRate(): Int {
        for (rate in PREFERRED_SAMPLE_RATES) {
            val size = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (size > 0) {
                if (rate != DEFAULT_AUDIO_SAMPLE_RATE) {
                    AppLogger.log("Using alt audio rate: $rate")
                }
                return rate
            }
        }
        return DEFAULT_AUDIO_SAMPLE_RATE
    }

    private fun pickAudioSource(): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val supportsUnprocessed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        } else false

        return when {
            supportsUnprocessed -> MediaRecorder.AudioSource.UNPROCESSED
            else -> MediaRecorder.AudioSource.MIC
        }
    }

    private fun startAudioEncodingLoop() {
        if (audioRecord == null || audioCodec == null) {
            AppLogger.log("Audio Loop Skipped: null rec/codec")
            return
        }
        
        executor.submit {
            // Fix: Set Thread Priority to URGENT_AUDIO
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            AppLogger.log("Audio Loop Thread Started (Urgent)")
            
            try {
                audioRecord?.startRecording()
                val bufferInfo = MediaCodec.BufferInfo()
                
                // Keep audio timestamps on the same monotonic clock as video
                var audioPresentationTimeUs = System.nanoTime() / 1000
                
                while (isRecording && audioCodec != null) {
                    // 1. Read PCM from Mic -> Input Buffer
                    val inputBufferIndex = audioCodec?.dequeueInputBuffer(10000) ?: -1
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = audioCodec?.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val readBytes = audioRecord?.read(inputBuffer, inputBuffer.remaining()) ?: 0
                            
                            if (readBytes > 0) {
                                audioCodec?.queueInputBuffer(inputBufferIndex, 0, readBytes, audioPresentationTimeUs, 0)
                                
                                // Calculate duration of this chunk in microseconds
                                // bytes * 1_000_000 / (SampleRate * Channels * BytesPerSample)
                                val durationUs = (readBytes * 1_000_000L) / (audioSampleRate * AUDIO_CHANNEL_COUNT * 2)
                                audioPresentationTimeUs += durationUs
                            } else if (readBytes < 0) {
                                Log.e(TAG, "AudioRecord read error: $readBytes")
                            } 
                        }
                    }
                    
                    // 2. Read AAC from Output Buffer -> CircularBuffer
                    val outputBufferIndex = audioCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                    if (outputBufferIndex >= 0) {
                         val encodedData = audioCodec?.getOutputBuffer(outputBufferIndex)
                         if (encodedData != null) {
                             if (bufferInfo.size > 0) {
                                 CircularBuffer.addFrame(encodedData, bufferInfo, CircularBuffer.FrameType.AUDIO)
                             }
                             audioCodec?.releaseOutputBuffer(outputBufferIndex, false)
                         }
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = audioCodec?.outputFormat
                        if (newFormat != null) {
                            CircularBuffer.audioFormat = newFormat
                            Log.d(TAG, "Audio Format Changed (Captured): $newFormat")
                        }
                    }
                }
            } catch (e: Exception) {
               Log.e(TAG, "Audio Loop Error", e)
               AppLogger.log("Audio Err: ${e.message}")
            } finally {
                Log.d(TAG, "Stopping Audio Record")
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioCodec?.stop()
                    audioCodec?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping audio", e)
                }
                audioCodec = null
            }
        }
    }

    private fun releaseAudioCodecSafely() {
        try {
            audioCodec?.stop()
        } catch (_: Exception) { }
        try {
            audioCodec?.release()
        } catch (_: Exception) { }
        audioCodec = null
    }

    fun stopCamera() {
        isRecording = false
        try {
            // Stop audio first to avoid blocking writes during teardown
            audioRecord?.stop()
        } catch (_: Exception) { }
        try {
            audioRecord?.release()
        } catch (_: Exception) { }
        audioRecord = null
        try {
            audioCodec?.stop()
        } catch (_: Exception) { }
        try {
            audioCodec?.release()
        } catch (_: Exception) { }
        audioCodec = null
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cameraProvider?.unbindAll()
        CircularBuffer.clear()
        executor.shutdown()
    }

    fun setLinearZoom(value: Float) {
        try {
            val clamped = value.coerceIn(0f, 1f)
            camera?.cameraControl?.setLinearZoom(clamped)
        } catch (_: Exception) {
            // Camera might not be ready; ignore
        }
    }
}
