package com.example.cameracontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

class VideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var isLooping = false
    private val CHUNK_DURATION_MS = 1000L // 1 second chunks

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isLooping) return
            restartRecording()
            // Schedule next restart
            mainHandler.postDelayed(this, CHUNK_DURATION_MS)
        }
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
            // Start the recording loop once camera is bound
            isLooping = true
            startRecording() // start first chunk
            mainHandler.postDelayed(loopRunnable, CHUNK_DURATION_MS)
            
        } catch (exc: Exception) {
            Log.e("VideoRecorder", "Use case binding failed", exc)
        }
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return

        // Create a unique file for this chunk
        // Format: [DEVICE_ID]_[TIMESTAMP].mp4
        val deviceId = DeviceIdentity.getDeviceId(context)
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val name = "${deviceId}_${timestamp}.mp4"
        val file = File(context.cacheDir, name)

        val outputOptions = FileOutputOptions.Builder(file).build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("VideoRecorder", "No Audio Permission")
            return
        }

        currentRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    if (!recordEvent.hasError()) {
                        val resultFile = File(recordEvent.outputResults.outputUri.path ?: "")
                        if (resultFile.exists()) {
                            BufferManager.addFile(resultFile)
                        } else {
                            // Fallback if URI path is tricky, though with FileOutputOptions it usually maps 1:1
                            BufferManager.addFile(file)
                        }
                    } else {
                        currentRecording?.close()
                        currentRecording = null
                        Log.e("VideoRecorder", "Video capture ended with error: ${recordEvent.error}")
                    }
                }
            }
    }
    
    private fun restartRecording() {
        val oldRecording = currentRecording
        currentRecording = null // prevent double stop logic if any
        oldRecording?.stop()
        
        // Immediate restart
        startRecording()
    }
    
    fun stopAll() {
        isLooping = false
        mainHandler.removeCallbacks(loopRunnable)
        currentRecording?.stop()
        currentRecording = null
        cameraExecutor.shutdown()
    }
}
