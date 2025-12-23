package com.example.cameracontrol

import android.content.Context

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraScreen()
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current

    // Permission State
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grantedMap ->
            hasPermissions = grantedMap.values.all { it }
            if (!hasPermissions) {
                Toast.makeText(context, "Permissions Required!", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        BufferManager.initialize(context) // Initialize the buffer manager
        AppLogger.log("App Started. Checking Permissions...")
        if (!hasPermissions) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        } else {
            // Already matched permissions, start connection
            val camPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            val micPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            AppLogger.log("Main Perms: Cam=$camPerm, Mic=$micPerm")
        }
    }
    
    // Also trigger if permissions granted later
    LaunchedEffect(hasPermissions) {
        // No-op here; service will handle networking when started
    }

    var cameraService by remember { mutableStateOf<CameraForegroundService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    DisposableEffect(hasPermissions) {
        if (!hasPermissions) {
            cameraService = null
            isBound = false
            return@DisposableEffect onDispose { }
        }

        val intent = Intent(context, CameraForegroundService::class.java).apply {
            action = CameraForegroundService.ACTION_START
        }

        ContextCompat.startForegroundService(context, intent)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? CameraForegroundService.CameraBinder
                cameraService = binder?.getService()
                isBound = binder != null
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                cameraService = null
                isBound = false
            }
        }

        val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isBound = bound

        onDispose {
            if (isBound) {
                context.unbindService(connection)
            }
            isBound = false
            cameraService = null
        }
    }

    val recorder = cameraService?.getRecorder()
    var zoomLinear by rememberSaveable { mutableFloatStateOf(0f) }

    // Debounce zoom updates to avoid overloading camera control
    LaunchedEffect(zoomLinear) {
        delay(60)
        recorder?.setLinearZoom(zoomLinear)
    }

    // Brief flash overlay whenever a WebSocket message is received
    LaunchedEffect(recorder) {
        if (recorder == null) return@LaunchedEffect
        NetworkClient.messageFlash.collectLatest {
            recorder.pulseTorch()
        }
    }

    // Keep torch on while disconnected to signal WS issues
    LaunchedEffect(recorder) {
        if (recorder == null) return@LaunchedEffect
        NetworkClient.connectionStatus.collectLatest { isConnected ->
            recorder.setTorchEnabled(!isConnected)
        }
    }
    
    if (hasPermissions && recorder != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        recorder.attachPreview(this.surfaceProvider)
                    }
                },
                update = { view ->
                    recorder.attachPreview(view.surfaceProvider)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // DEBUG LOGS OVERLAY
            var showLogs by remember { mutableStateOf(false) }

            if (showLogs) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text("Debug Logs:", color = Color.White, fontSize = 14.sp)
                    LazyColumn(reverseLayout = false) {
                        items(AppLogger.logs) { log ->
                            Text(text = log, color = Color.Green, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            // Log Build Time on startup
            LaunchedEffect(Unit) {
                AppLogger.log("Build Time: " + BuildConfig.BUILD_TIME)
            }

            // Bottom controls: zoom + trigger
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Zoom Control (Full Width)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1E1E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Zoom ${(zoomLinear * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Slider(
                            value = zoomLinear,
                            onValueChange = { value -> zoomLinear = value },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Toggle Logs Button
                    Button(
                        onClick = { showLogs = !showLogs },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text(if (showLogs) "Hide Logs" else "Show Logs")
                    }

                    Button(
                        onClick = {
                            AppLogger.log("Manual Trigger Clicked")
                            BufferManager.triggerUpload(System.currentTimeMillis().toString())
                        },
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text("Simulate")
                    }
                }
            }
        }
    } else if (!hasPermissions) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please grant camera and audio permissions to continue.")
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Starting camera service...")
        }
    }
}
