package com.example.cameracontrol

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    
    // KEEP SCREEN ON
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }
    
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
            
            AppLogger.log("Permissions OK. Connecting WS...")
            NetworkClient.connectWebSocket()
        }
    }
    
    // Also trigger if permissions granted later
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            NetworkClient.connectWebSocket()
        }
    }

    val recorder = remember { VideoRecorder(context, lifecycleOwner) }

    // Ensure recorder is fully stopped when the composable leaves the tree
    DisposableEffect(Unit) {
        onDispose {
            AppLogger.log("Disposing recorder")
            recorder.stopCamera()
        }
    }
    
    if (hasPermissions) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        recorder.startCamera(this.surfaceProvider)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // DEBUG LOGS OVERLAY
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

            // Trigger Button
            Button(
                onClick = { 
                    AppLogger.log("Manual Trigger Clicked")
                    BufferManager.triggerUpload() 
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text("Simulate Server Request")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please grant camera and audio permissions to continue.")
        }
    }
}
