package com.example.cameracontrol

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
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
    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var isRecording = false
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "VideoRecorder"
        // 720p is good for performance/quality balance
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        private const val BIT_RATE = 2_500_000 // 2.5 Mbps
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1 // Keyframe every 1 second for easier cutting
    }

    fun startCamera(surfaceProvider: Preview.SurfaceProvider) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // 1. UI Preview (Viewfinder)
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(surfaceProvider)
                }

            // 2. Prepare Encoder & Input Surface
            setupMediaCodec()
            
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
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    encodingPreview
                )
                
                startEncodingLoop()
                isRecording = true
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

    private fun startEncodingLoop() {
        executor.submit {
            val bufferInfo = MediaCodec.BufferInfo()
            while (isRecording && mediaCodec != null) {
                try {
                    val encoderStatus = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1

                    if (encoderStatus >= 0) {
                        val encodedData = mediaCodec?.getOutputBuffer(encoderStatus)
                        if (encodedData != null) {
                            // Send to CircularBuffer
                            if (bufferInfo.size > 0) {
                                CircularBuffer.addFrame(encodedData, bufferInfo)
                            }
                            mediaCodec?.releaseOutputBuffer(encoderStatus, false)
                        }
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // This usually contains the CSD-0/CSD-1 (SPS/PPS) which are critical
                        val newFormat = mediaCodec?.outputFormat
                        Log.d(TAG, "Output format changed: $newFormat")
                        if (newFormat != null) {
                            CircularBuffer.mediaFormat = newFormat
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in encoding loop", e)
                }
            }
        }
    }

    fun stopCamera() {
        isRecording = false
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cameraProvider?.unbindAll()
        executor.shutdown()
    }
}
