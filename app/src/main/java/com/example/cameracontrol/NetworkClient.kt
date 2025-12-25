package com.example.cameracontrol

import android.util.Log
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkClient {
    private const val TAG = "NetworkClient"
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnecting = false
    private val handler = Handler(Looper.getMainLooper())
    private const val RECONNECT_DELAY_MS = 3000L

    // REPLACE THIS WITH YOUR COMPUTER'S LOCAL IP if testing on phone
    // 10.0.2.2 is "localhost" for Android Emulator
    private const val BASE_URL = "https://camera.boxhoster.com"
    private const val WS_URL = "wss://camera.boxhoster.com/cable"

    private val _messageFlash = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val messageFlash = _messageFlash.asSharedFlow()
    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus = _connectionStatus.asStateFlow()

    private var currentRoomId: String? = null
    
    // Public getter for current room ID
    fun getCurrentRoomId(): String? = currentRoomId

    @Synchronized
    fun connectWebSocket(roomId: String? = null) {
        if (roomId != null) {
            currentRoomId = roomId
        }
        
        if (isConnecting || _connectionStatus.value) {
            AppLogger.log("WS already connecting/connected")
            return
        }
        isConnecting = true
        AppLogger.log("Opening WS (Room: ${currentRoomId ?: "Default"})...")

        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Origin", BASE_URL)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                AppLogger.log("WS Connected")
                _connectionStatus.value = true
                isConnecting = false
                handler.removeCallbacksAndMessages(null)
                subscribeToChannel(webSocket, currentRoomId)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")

                    if (type == "ping") {
                        // ActionCable pings are just heartbeat, ignore them
                        return
                    }
                    else if (type == "welcome") {
                        AppLogger.log("WS Welcome")
                    }
                    else if (json.has("message")) {
                        // Check if message is a JSON object (data payload)
                        val messageObj = json.optJSONObject("message")
                        if (messageObj != null) {
                             val action = messageObj.optString("action")
                             if (action == "capture") {
                                _messageFlash.tryEmit(Unit) // Trigger UI flash only on capture requests
                                AppLogger.log("CAPTURE SIGNAL RECEIVED!")
                                val timestamp = messageObj.optString("timestamp", "").ifBlank { null }
                                BufferManager.triggerUpload(timestamp)
                            }
                        } else {
                            // Message might be a string or int (like a ping timestamp inside a non-standard msg)
                            // Just ignore for now
                        }
                    } else {
                        // Other messages
                    }
                } catch (e: Exception) {
                    // Only log real errors, not parsing pings
                    if (!text.contains("ping")) {
                         AppLogger.log("WS Parse Err: ${e.message}")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                AppLogger.log("WS Failure: ${t.message}")
                handleDisconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                AppLogger.log("WS Closed: $reason")
                handleDisconnect()
            }
        })
    }

    /**
     * Manual trigger used by the UI (Simulate button) to mimic an incoming capture signal.
     * This emits the same `messageFlash` event as a real WS 'capture' message and
     * then invokes the buffer upload flow with the provided timestamp.
     */
    fun manualTrigger(triggerTimestamp: String? = null) {
        _messageFlash.tryEmit(Unit)
        AppLogger.log("Manual Trigger: emitting flash + starting upload")
        BufferManager.triggerUpload(triggerTimestamp)
    }

    private fun handleDisconnect() {
        _connectionStatus.value = false
        isConnecting = false
        webSocket = null
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (isConnecting || _connectionStatus.value) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            AppLogger.log("Reconnecting WS...")
            connectWebSocket(currentRoomId) // Reconnect with same room
        }, RECONNECT_DELAY_MS)
    }

    @Synchronized
    fun disconnectWebSocket() {
        AppLogger.log("Disconnecting WS...")
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "Service stopped")
        webSocket = null
        _connectionStatus.value = false
        isConnecting = false
        currentRoomId = null
    }

    private fun subscribeToChannel(ws: WebSocket, roomId: String?) {
        val subscribeMsg = JSONObject()
        subscribeMsg.put("command", "subscribe")
        
        // Construct Identifier: {"channel":"RecordingChannel", "room":"<id>"}
        val identifier = JSONObject()
        identifier.put("channel", "RecordingChannel")
        if (!roomId.isNullOrBlank()) {
            identifier.put("room", roomId)
        }
        
        subscribeMsg.put("identifier", identifier.toString())
        ws.send(subscribeMsg.toString())
        AppLogger.log("Subscribing to RecordingChannel (Room=${roomId ?: "Public"})...")
    }

    fun uploadFile(file: File, remoteFileName: String, timestamp: String?, room: String? = null, onComplete: () -> Unit) {
        val mediaType = "video/mp4".toMediaType()
        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "video",
                remoteFileName, // Use the clean name for the server
                file.asRequestBody(mediaType)
            )
        timestamp?.let { requestBodyBuilder.addFormDataPart("timestamp", it) }
        room?.let { requestBodyBuilder.addFormDataPart("room", it) }
        val requestBody = requestBodyBuilder.build()

        AppLogger.log("Uploading: $remoteFileName...")

        val request = Request.Builder()
            .url("$BASE_URL/recorder/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                AppLogger.log("Upload Fail: ${e.message}")
                onComplete() // Clean up even on fail
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    AppLogger.log("Upload Success: ${file.name}")
                } else {
                    AppLogger.log("Upload Err: ${response.code}")
                }
                response.close()
                onComplete()
            }
        })
    }
}
