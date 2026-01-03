package com.example.cameracontrol

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.atomic.AtomicBoolean

class TorchController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val handlerThread = HandlerThread("TorchController")
    private val handler: Handler
    private val torchOn = AtomicBoolean(false)
    private val blinking = AtomicBoolean(false)
    @Volatile private var blinkRunnable: Runnable? = null
    @Volatile private var pulseRestore: Runnable? = null
    private val cameraId: String?

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        cameraId = findCameraWithFlash()
        AppLogger.log(TAG, "Torch controller init: cameraId=${cameraId ?: "none"}")
    }

    fun pulse(durationMs: Long = 200) {
        val id = cameraId ?: run {
            AppLogger.log(TAG, "Torch unavailable: no camera with flash")
            return
        }
        handler.post {
            cancelBlinkInternal("pulse")
            pulseRestore?.let { handler.removeCallbacks(it) }
            val wasEnabled = torchOn.get()
            if (wasEnabled) {
                setTorchInternal(id, false, "pulse pre-off")
            }
            setTorchInternal(id, true, "pulse on")
            val restore = Runnable {
                setTorchInternal(id, wasEnabled, "pulse restore")
            }
            pulseRestore = restore
            handler.postDelayed(restore, durationMs)
        }
    }

    fun blink(pulses: Int = 3, onMs: Long = 120, offMs: Long = 120) {
        val id = cameraId ?: run {
            AppLogger.log(TAG, "Torch unavailable: no camera with flash")
            return
        }
        if (!blinking.compareAndSet(false, true)) {
            AppLogger.log(TAG, "Torch blink skipped: already blinking")
            return
        }
        handler.post {
            pulseRestore?.let { handler.removeCallbacks(it) }
            val wasEnabled = torchOn.get()
            val totalSteps = (pulses.coerceAtLeast(1) * 2)
            var step = 0
            val runner = object : Runnable {
                override fun run() {
                    if (step >= totalSteps || !blinking.get()) {
                        setTorchInternal(id, wasEnabled, "blink restore")
                        blinking.set(false)
                        return
                    }
                    val enable = step % 2 == 0
                    setTorchInternal(id, enable, "blink step=$step")
                    val delay = if (enable) onMs else offMs
                    step += 1
                    handler.postDelayed(this, delay)
                }
            }
            blinkRunnable = runner
            runner.run()
        }
    }

    fun setEnabled(enabled: Boolean) {
        val id = cameraId ?: run {
            AppLogger.log(TAG, "Torch unavailable: no camera with flash")
            return
        }
        handler.post {
            cancelBlinkInternal("setEnabled")
            pulseRestore?.let { handler.removeCallbacks(it) }
            setTorchInternal(id, enabled, "setEnabled")
        }
    }

    fun cancelBlink(reason: String) {
        handler.post {
            cancelBlinkInternal(reason)
        }
    }

    private fun cancelBlinkInternal(reason: String) {
        if (!blinking.compareAndSet(true, false)) {
            return
        }
        blinkRunnable?.let { handler.removeCallbacks(it) }
        blinkRunnable = null
        AppLogger.log(TAG, "Torch blink cancelled ($reason)")
    }

    private fun setTorchInternal(cameraId: String, enabled: Boolean, reason: String) {
        try {
            cameraManager.setTorchMode(cameraId, enabled)
            torchOn.set(enabled)
            AppLogger.log(TAG, "Torch ${if (enabled) "ON" else "OFF"} ($reason)")
        } catch (e: Exception) {
            AppLogger.log(TAG, "Torch set error ($reason): ${e.message}")
        }
    }

    private fun findCameraWithFlash(): String? {
        try {
            val ids = cameraManager.cameraIdList
            for (id in ids) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id
                }
            }
            for (id in ids) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) return id
            }
        } catch (e: Exception) {
            AppLogger.log(TAG, "Torch camera lookup error: ${e.message}")
        }
        return null
    }

    companion object {
        private const val TAG = "TorchController"
    }
}
