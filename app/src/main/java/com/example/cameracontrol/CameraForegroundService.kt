package com.example.cameracontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService

class CameraForegroundService : LifecycleService() {
    companion object {
        const val CHANNEL_ID = "camera_recorder_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.example.cameracontrol.START"
        const val ACTION_STOP = "com.example.cameracontrol.STOP"
    }

    inner class CameraBinder : Binder() {
        fun getService(): CameraForegroundService = this@CameraForegroundService
    }

    private val binder = CameraBinder()
    private lateinit var recorder: VideoRecorder
    private var wakeLock: PowerManager.WakeLock? = null
    private var hasStartedCamera = false

    override fun onBind(intent: Intent): android.os.IBinder? = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        acquireWakeLock()
        recorder = VideoRecorder(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure lifecycle dispatch inside LifecycleService
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (!hasStartedCamera) {
            val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!cameraGranted || !audioGranted) {
                AppLogger.log("Foreground service missing permissions, stopping")
                stopSelf()
                return START_NOT_STICKY
            }

            NetworkClient.connectWebSocket()
            recorder.startCamera()
            hasStartedCamera = true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (this::recorder.isInitialized) {
            recorder.stopCamera()
        }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    fun getRecorder(): VideoRecorder = recorder

    fun attachPreview(surfaceProvider: androidx.camera.core.Preview.SurfaceProvider) {
        recorder.attachPreview(surfaceProvider)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, CameraForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Camera recording")
            .setContentText("Recording continues with screen off")
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CameraControl::RecorderLock").apply {
            setReferenceCounted(false)
            acquire()
        }
    }
}
