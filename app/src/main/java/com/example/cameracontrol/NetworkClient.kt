package com.example.cameracontrol

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val TAG = "NetworkClient"
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    
    // REPLACE THIS WITH YOUR COMPUTER'S LOCAL IP if testing on phone
    // 10.0.2.2 is "localhost" for Android Emulator
    private const val BASE_URL = "https://chemical-topics-steady-morris.trycloudflare.com" 
    private const val WS_URL = "wss://chemical-topics-steady-morris.trycloudflare.com/cable"

    fun connectWebSocket() {
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Origin", BASE_URL)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                AppLogger.log("WS Connected")
                subscribeToChannel(webSocket)
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
                             if (messageObj.optString("action") == "capture") {
                                AppLogger.log("CAPTURE SIGNAL RECEIVED!")
                                BufferManager.triggerUpload()
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
                reconnect()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                AppLogger.log("WS Closed: $reason")
                reconnect()
            }
        })
    }

    private fun reconnect() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            AppLogger.log("Reconnecting WS...")
            connectWebSocket()
        }, 3000)
    }

    private fun subscribeToChannel(ws: WebSocket) {
        val subscribeMsg = JSONObject()
        subscribeMsg.put("command", "subscribe")
        subscribeMsg.put("identifier", "{\"channel\":\"RecordingChannel\"}")
        ws.send(subscribeMsg.toString())
        AppLogger.log("Subscribing to RecordingChannel...")
    }

    fun uploadFile(file: File, remoteFileName: String, onComplete: () -> Unit) {
        val mediaType = "video/mp4".toMediaType()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "video", 
                remoteFileName, // Use the clean name for the server
                file.asRequestBody(mediaType)
            )
            .build()
            
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
