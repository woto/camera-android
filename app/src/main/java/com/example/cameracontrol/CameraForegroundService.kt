package com.example.cameracontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraForegroundService : LifecycleService() {
    companion object {
        const val CHANNEL_ID = "camera_recorder_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.example.cameracontrol.START"
        const val ACTION_STOP = "com.example.cameracontrol.STOP"

        private val _foregroundState = MutableStateFlow(false)
        val foregroundState: StateFlow<Boolean> = _foregroundState
    }

    inner class CameraBinder : Binder() {
        fun getService(): CameraForegroundService = this@CameraForegroundService
    }

    private val binder = CameraBinder()
    private lateinit var recorder: VideoRecorder
    private var wakeLock: PowerManager.WakeLock? = null
    private var hasStartedCamera = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!this@CameraForegroundService::recorder.isInitialized) return
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> recorder.setScreenOn(true)
                Intent.ACTION_SCREEN_OFF -> recorder.setScreenOn(false)
            }
        }
    }

    override fun onBind(intent: Intent): android.os.IBinder? = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        _foregroundState.value = true
        acquireWakeLock()
        recorder = VideoRecorder(applicationContext, this)
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure lifecycle dispatch inside LifecycleService
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                AppLogger.log("Svc Stopping...")
                // Stop foreground mode first - this is critical!
                stopForeground(STOP_FOREGROUND_REMOVE)
                _foregroundState.value = false
                // Disconnect WebSocket
                NetworkClient.disconnectWebSocket()
                // Reset flag so service can restart cleanly
                hasStartedCamera = false
                // Stop service
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                _foregroundState.value = true
            }
        }

        if (!hasStartedCamera) {
            val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!cameraGranted || !audioGranted) {
                AppLogger.log("Foreground service missing permissions, stopping")
                _foregroundState.value = false
                stopSelf()
                return START_NOT_STICKY
            }

            val roomId = intent?.getStringExtra("room_id")
            AppLogger.log("Svc Starting (Room=$roomId)")
            NetworkClient.connectWebSocket(roomId)
            recorder.startCamera()
            hasStartedCamera = true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (this::recorder.isInitialized) {
            recorder.destroy()
        }
        NetworkClient.disconnectWebSocket()
        hasStartedCamera = false
        _foregroundState.value = false
        wakeLock?.let { if (it.isHeld) it.release() }
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) { }
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
