package com.example.cameracontrol

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaActionSound
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.Surface
import android.view.OrientationEventListener
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

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
    private var boundPreviewProvider: Preview.SurfaceProvider? = null
    private var torchState: Boolean? = null
    private val shutterSound: MediaActionSound = MediaActionSound().apply {
        // Preload to avoid latency on first play
        load(MediaActionSound.SHUTTER_CLICK)
    }
    private var activeWidth = WIDTH
    private var activeHeight = HEIGHT
    @Volatile private var sessionRotationDegrees: Int = 0
    @Volatile private var isShutterSoundReleased = false
    @Volatile private var lastKnownDisplayRotation: Int? = null
    @Volatile private var lastPreviewRotation: Int? = null
    @Volatile private var isScreenOn: Boolean = true
    @Volatile private var isStarting = false
    private var orientationListener: OrientationEventListener? = null
    private val isAudioStopping = AtomicBoolean(false)
    
    companion object {
        private const val TAG = "VideoRecorder"
        // Push encoder up to 1080p for better clarity
        private const val WIDTH = 1920
        private const val HEIGHT = 1080
        private const val BIT_RATE = 10_000_000 // 10 Mbps
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1 // Keyframe every 1 second for easier cutting
        
        // Audio Config
        private const val DEFAULT_AUDIO_SAMPLE_RATE = 48000
        private const val AUDIO_BIT_RATE = 64000
        private const val AUDIO_CHANNEL_COUNT = 1 // Mono
        private val PREFERRED_SAMPLE_RATES = listOf(48000, 44100, 32000, 16000)
    }


    @Suppress("DEPRECATION")
    fun startCamera(surfaceProvider: Preview.SurfaceProvider? = null) {
        surfaceProvider?.let { boundPreviewProvider = it }
        if (isStarting) {
            AppLogger.log(TAG, "startCamera() ignored: already starting")
            return
        }
        if (isRecording) {
            AppLogger.log(TAG, "startCamera() ignored: already recording")
            return
        }
        isStarting = true
        AppLogger.log(TAG, "startCamera() boundPreviewProvider=${boundPreviewProvider != null}")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                AppLogger.log(TAG, "Camera Provider Ready")
                cameraProvider = cameraProviderFuture.get()

                startOrientationListenerIfNeeded()

                val rotation = lastKnownDisplayRotation ?: safeDisplayRotation() ?: Surface.ROTATION_0
                val previewRotation = lastPreviewRotation ?: safeDisplayRotation() ?: rotation
                AppLogger.log(
                    TAG,
                    "Orientation pick: lastKnownDisplayRotation=$lastKnownDisplayRotation " +
                        "safeDisplayRotation=${safeDisplayRotation()} lastPreviewRotation=$lastPreviewRotation " +
                        "rotation=$rotation previewRotation=$previewRotation"
                )
                val targetSize = resolveTargetSize(rotation)
                activeWidth = targetSize.width
                activeHeight = targetSize.height
                AppLogger.log(
                    TAG,
                    "Starting Camera with Resolution: ${activeWidth}x${activeHeight} " +
                        "(rotation=$rotation previewRotation=$previewRotation)"
                )

                val cameraInfo = cameraProvider?.availableCameraInfos?.firstOrNull {
                    CameraSelector.DEFAULT_BACK_CAMERA.filter(listOf(it)).isNotEmpty()
                }
                val baseRotation = cameraInfo?.let { info ->
                    info.getSensorRotationDegrees(rotation)
                } ?: 0
                val needsLandscapeFlip = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
                sessionRotationDegrees = if (needsLandscapeFlip) {
                    (baseRotation + 180) % 360
                } else {
                    baseRotation
                }
                AppLogger.log(
                    TAG,
                    "Orientation calc: baseRotation=$baseRotation needsLandscapeFlip=$needsLandscapeFlip " +
                        "sessionRotationDegrees=$sessionRotationDegrees"
                )
                CircularBuffer.rotationDegrees = sessionRotationDegrees
                CircularBuffer.clear()
                AppLogger.log(TAG, "Session rotation degrees=$sessionRotationDegrees (base=$baseRotation)")

                // 1. UI Preview (Viewfinder) - only when screen is on
                // Sync UI resolution with Recording resolution (1080p) to ensure stream compatibility
                preview = if (isScreenOn) {
                    Preview.Builder()
                        .setTargetResolution(targetSize)
                        .setTargetRotation(previewRotation)
                        .build()
                } else {
                    null
                }

                // 2. Prepare Encoder & Input Surface
                setupMediaCodec()       // Video
                setupAudioMediaCodec()  // Audio
                
                // 3. Recorder Preview (Feeds the Encoder)
                encodingPreview = Preview.Builder()
                    .setTargetName("EncodingPreview")
                    .setTargetResolution(targetSize)
                    .setTargetRotation(previewRotation)
                    .build()

                // We need to bridge the Encoder's Surface to this Preview
                // Once the codec is configured, inputSurface is ready.
                encodingPreview?.setSurfaceProvider { request ->
                    if (inputSurface != null) {
                        request.provideSurface(inputSurface!!, executor) { result -> 
                            // Surface release callback
                            AppLogger.log(TAG, "Encoding surface request result: ${result.resultCode}")
                        }
                    } else {
                        request.willNotProvideSurface()
                    }
                }

                try {
                    cameraProvider?.unbindAll()
                    // Bind both the UI preview and the Encoding preview
                    // If boundPreviewProvider is null, Preview will run but not show anything until attachPreview is called
                    AppLogger.log(TAG, "Binding Camera UseCases...")
                    val useCases = if (preview != null) {
                        arrayOf(preview, encodingPreview)
                    } else {
                        arrayOf(encodingPreview)
                    }
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        *useCases
                    )
                    AppLogger.log(TAG, "Camera bound. preview=${preview != null} encodingPreview=${encodingPreview != null}")
                    
                    // If UI is already waiting, attach it
                    // If UI is already waiting, attach it
                    boundPreviewProvider?.let { 
                        AppLogger.log(TAG, "Attaching UI Surface to Preview")
                        preview?.setSurfaceProvider(it)
                    }

                    BufferManager.setRotationProvider { sessionRotationDegrees }
                    
                    isRecording = true
                    startEncodingLoop()         // Video Loop
                    // AppLogger.log(TAG, "Starting Audio Loop...")
                    startAudioEncodingLoop()    // Audio Loop
                    
                    AppLogger.log(TAG, "Camera and Recording started seamlessly")
                    
                } catch (exc: Exception) {
                    AppLogger.e(TAG, "Use case binding failed", exc)
                    AppLogger.log(TAG, "Camera Bind Failed: ${exc.message}")
                }
            } finally {
                isStarting = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun attachPreview(surfaceProvider: Preview.SurfaceProvider, displayRotation: Int? = null) {
        boundPreviewProvider = surfaceProvider
        if (preview == null) {
            if (isScreenOn) {
                AppLogger.log(TAG, "Preview missing; restarting camera to attach UI")
                startCamera(surfaceProvider)
            } else {
                AppLogger.log(TAG, "Preview not ready; screen off, skip attach")
            }
        } else {
            preview?.setSurfaceProvider(surfaceProvider)
            val rotation = displayRotation ?: safeDisplayRotation() ?: lastKnownDisplayRotation ?: Surface.ROTATION_0
            lastPreviewRotation = rotation
            preview?.targetRotation = rotation
            encodingPreview?.targetRotation = rotation
            AppLogger.log(TAG, "attachPreview() rotation=$rotation")
        }
    }

    fun setScreenOn(isOn: Boolean) {
        if (isScreenOn == isOn) return
        isScreenOn = isOn
        AppLogger.log(TAG, "Screen state: ${if (isOn) "ON" else "OFF"}")
        try {
            stopCamera()
            startCamera(boundPreviewProvider)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error restarting camera on screen state change", e)
        }
    }

    fun pulseTorch(durationMs: Long = 200) {
        val cam = camera ?: run {
            AppLogger.log(TAG, "Torch skipped: camera not ready")
            return
        }
        val info = cam.cameraInfo
        if (!info.hasFlashUnit()) {
            AppLogger.log(TAG, "Torch unavailable on this device")
            return
        }
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val wasEnabled = torchState == true
                if (wasEnabled) {
                    cam.cameraControl.enableTorch(false)
                    torchState = false
                }
                cam.cameraControl.enableTorch(true)
                torchState = true
                if (!isShutterSoundReleased) {
                    shutterSound.play(MediaActionSound.SHUTTER_CLICK)
                }
                mainHandler.postDelayed({
                    if (wasEnabled) {
                        cam.cameraControl.enableTorch(true)
                        torchState = true
                    } else {
                        cam.cameraControl.enableTorch(false)
                        torchState = false
                    }
                }, durationMs.toLong())
            } catch (e: Exception) {
                AppLogger.log(TAG, "Torch error: ${e.message}")
            }
        }
    }

    fun setTorchEnabled(enabled: Boolean) {
        val cam = camera ?: run {
            AppLogger.log(TAG, "Torch skipped: camera not ready")
            return
        }
        val info = cam.cameraInfo
        if (!info.hasFlashUnit()) {
            AppLogger.log(TAG, "Torch unavailable on this device")
            return
        }
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            if (torchState == enabled) return@post
            try {
                cam.cameraControl.enableTorch(enabled)
                torchState = enabled
                AppLogger.log(TAG, "Torch ${if (enabled) "ON" else "OFF"}")
            } catch (e: Exception) {
                AppLogger.log(TAG, "Torch error: ${e.message}")
            }
        }
    }

    private fun setupMediaCodec() {
        try {
            try {
                inputSurface?.release()
            } catch (_: Exception) { }
            inputSurface = null
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, activeWidth, activeHeight)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            AppLogger.log(TAG, "Video codec format: $format")

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            // Create the input surface *after* configure and *before* start
            inputSurface = mediaCodec?.createInputSurface()
            
            mediaCodec?.start()
        } catch (e: IOException) {
            AppLogger.e(TAG, "Failed to create MediaCodec", e)
            AppLogger.log(TAG, "Codec Error: ${e.message}")
        }
    }
    
    private fun setupAudioMediaCodec() {
        AppLogger.log(TAG, "Setting up Audio Codec...")
        try {
            isAudioStopping.set(false)
            audioSampleRate = resolveSampleRate()
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioSampleRate, AUDIO_CHANNEL_COUNT)
            format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            AppLogger.log(TAG, "Audio codec format: $format")

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
                AppLogger.e(TAG, "Unsupported audio config. Min buffer=$minBufferSize at rate=$audioSampleRate")
                AppLogger.log(TAG, "Audio cfg not supported")
                releaseAudioCodecSafely()
                return
            }
            
            val perm = android.Manifest.permission.RECORD_AUDIO
            val check = ContextCompat.checkSelfPermission(context, perm)
            AppLogger.log(
                TAG,
                "Checking Perm: $perm on ${context.packageName}. Result: $check " +
                    "(Expected: ${android.content.pm.PackageManager.PERMISSION_GRANTED})"
            )

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
                    AppLogger.e(TAG, "AudioRecord failed to initialize!")
                    AppLogger.log(TAG, "AudioRecord Init Failed")
                    releaseAudioCodecSafely()
                    audioRecord = null
                } else {
                    AppLogger.log(TAG, "AudioRecord initialized (src=$audioSource, rate=$audioSampleRate, buf=$bufferSize)")
                }
            } else {
                AppLogger.e(TAG, "RECORD_AUDIO Permission NOT GRANTED")
                AppLogger.log(TAG, "No Audio Perm")
                releaseAudioCodecSafely()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to create AudioCodec", e)
            AppLogger.log(TAG, "Audio Codec Err: ${e.message}")
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
                        AppLogger.log(TAG, "Output format changed: $newFormat")
                        if (newFormat != null) {
                            CircularBuffer.videoFormat = newFormat
                            AppLogger.log(TAG, "Video format ready")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error in encoding loop", e)
                    AppLogger.log(TAG, "Video loop err: ${e.message}")
                }
            }
        }
    }

    private fun resolveSampleRate(): Int {
        for (rate in PREFERRED_SAMPLE_RATES) {
            val size = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (size > 0) {
                if (rate != DEFAULT_AUDIO_SAMPLE_RATE) {
                    AppLogger.log(TAG, "Using alt audio rate: $rate")
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
            AppLogger.log(TAG, "Audio Loop Skipped: null rec/codec")
            return
        }
        
        executor.submit {
            // Fix: Set Thread Priority to URGENT_AUDIO
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            AppLogger.log(TAG, "Audio Loop Thread Started (Urgent)")
            
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
                                AppLogger.e(TAG, "AudioRecord read error: $readBytes")
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
                            AppLogger.log(TAG, "Audio Format Changed (Captured): $newFormat")
                        }
                    }
                }
            } catch (e: Exception) {
               AppLogger.e(TAG, "Audio Loop Error", e)
               AppLogger.log(TAG, "Audio Err: ${e.message}")
            } finally {
                shutdownAudio("audio loop end")
            }
        }
    }

    private fun shutdownAudio(reason: String) {
        if (!isAudioStopping.compareAndSet(false, true)) {
            return
        }
        AppLogger.log(TAG, "Stopping Audio Record ($reason)")
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: IllegalStateException) {
            AppLogger.e(TAG, "Error stopping audio record", e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping audio record", e)
        } finally {
            try {
                audioRecord?.release()
            } catch (_: Exception) { }
            audioRecord = null
        }
        try {
            audioCodec?.stop()
        } catch (e: IllegalStateException) {
            AppLogger.e(TAG, "Error stopping audio codec", e)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping audio codec", e)
        } finally {
            try {
                audioCodec?.release()
            } catch (_: Exception) { }
            audioCodec = null
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
        AppLogger.log(TAG, "stopCamera() called")
        isRecording = false
        isStarting = false
        // Stop audio first to avoid blocking writes during teardown
        shutdownAudio("stopCamera")
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            inputSurface?.release()
        } catch (_: Exception) { }
        inputSurface = null
        cameraProvider?.unbindAll()
        CircularBuffer.clear()
        AppLogger.log(TAG, "Camera stopped; buffer cleared")
        // executor.shutdown() -> MOVED TO destroy()
        // Keep shutter sound alive for quick restarts; release in destroy().
        try {
        } catch (_: Exception) { }
    }

    fun destroy() {
        stopCamera()
        try {
            if (!executor.isShutdown) {
                executor.shutdown()
            }
        } catch (_: Exception) { }
        try {
             orientationListener?.disable()
        } catch (_: Exception) { }
        try {
            if (!isShutterSoundReleased) {
                shutterSound.release()
                isShutterSoundReleased = true
            }
        } catch (_: Exception) { }
    }

    private fun safeDisplayRotation(): Int? {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        return try {
            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay?.rotation
            } ?: displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.rotation
            AppLogger.log(TAG, "safeDisplayRotation() -> $rotation")
            rotation
        } catch (_: Exception) {
            AppLogger.log(TAG, "safeDisplayRotation() failed; using lastKnownDisplayRotation=$lastKnownDisplayRotation")
            lastKnownDisplayRotation
        }
    }

    private fun resolveTargetSize(rotation: Int): Size {
        val isPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
        val desired = if (isPortrait) Size(HEIGHT, WIDTH) else Size(WIDTH, HEIGHT)
        AppLogger.log(TAG, "resolveTargetSize(): rotation=$rotation isPortrait=$isPortrait desired=${desired.width}x${desired.height}")
        val cameraInfo = cameraProvider?.availableCameraInfos?.firstOrNull {
            CameraSelector.DEFAULT_BACK_CAMERA.filter(listOf(it)).isNotEmpty()
        } ?: return desired
        val map = Camera2CameraInfo.from(cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return desired
        val sizes = map.getOutputSizes(SurfaceTexture::class.java) ?: return desired
        val desiredRatio = desired.width.toFloat() / desired.height.toFloat()
        val exact = sizes.firstOrNull { it.width == desired.width && it.height == desired.height }
        if (exact != null) {
            AppLogger.log(TAG, "Target size exact match: ${desired.width}x${desired.height}")
            return desired
        }
        val best = sizes
            .filter { abs(it.width.toFloat() / it.height.toFloat() - desiredRatio) < 0.01f }
            .maxByOrNull { it.width * it.height }
        if (best != null) {
            AppLogger.log(TAG, "Target size best match: ${best.width}x${best.height}")
            return Size(best.width, best.height)
        }
        AppLogger.log(TAG, "Target size fallback: ${WIDTH}x${HEIGHT}")
        return Size(WIDTH, HEIGHT)
    }

    private fun startOrientationListenerIfNeeded() {
        if (orientationListener != null) return

        orientationListener = object : OrientationEventListener(context.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val displayRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_90
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_270
                    else -> Surface.ROTATION_0
                }

                AppLogger.log(TAG, "Sensor orientation=$orientation -> displayRotation=$displayRotation")
                if (lastKnownDisplayRotation == displayRotation) return
                lastKnownDisplayRotation = displayRotation
                AppLogger.log(TAG, "Rotation changed to $displayRotation. Restarting camera...")
                try {
                    stopCamera()
                    startCamera(boundPreviewProvider)
                } catch(e: Exception) {
                    AppLogger.e(TAG, "Error restarting camera on rotation", e)
                    AppLogger.log(TAG, "Restart err: ${e.message}")
                }
            }
        }.also { listener ->
            try {
                if (listener.canDetectOrientation()) listener.enable()
            } catch (_: Exception) { }
        }
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
