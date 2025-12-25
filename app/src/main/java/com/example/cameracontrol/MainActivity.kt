package com.example.cameracontrol

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.layout
import androidx.compose.ui.composed
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive Mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        
        // Set up continuous system UI monitoring - automatically re-hide when bars appear
        @Suppress("DEPRECATION")
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            // When system UI becomes visible (fullscreen flag is not set), hide it again
            if ((visibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                hideSystemBars()
            }
        }
        
        hideSystemBars()
        
        setContent {
            MainApp()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Post to ensure it runs after layout changes complete
        window.decorView.post {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Additional flags for maximum stability
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
}

enum class AppStep {
    INTRO,
    ROOM_ID,
    CAMERA
}

// Simple Localization Helper
object Strings {
    fun get(id: String, isRussian: Boolean): String {
        return if (isRussian) mapRu[id] ?: id else mapEn[id] ?: id
    }

    private val mapEn = mapOf(
        "welcome" to "Welcome",
        "intro_text" to """
            This application is designed to record beautiful game moments in volleyball.

            The application uses the camera. Recording can be done even with the screen off to save battery. Stop recording after you finish using the phone.
        """.trimIndent(),
        "ok" to "OK",
        "room_id_title" to "Room ID",
        "room_id_prompt" to "Enter the Room ID for this session:",
        "permissions_required" to "Permissions Required!",
        "camera_permissions_msg" to "Please grant camera and audio permissions to continue.",
        "starting_camera" to "Starting camera service...",
        "debug_logs" to "Debug Logs:",
        "hide_logs" to "Hide Logs",
        "show_logs" to "Show Logs",
        "simulate" to "Simulate",
        "zoom" to "Zoom",
        "start_camera" to "Turn Camera ON",
        "stop_camera" to "Turn Camera OFF",
        "camera_disabled" to "Camera is Disabled"
    )

    private val mapRu = mapOf(
        "welcome" to "Добро пожаловать",
        "intro_text" to """
            Данное приложение предназначено для записи красивых игровых моментов в волейболе.

            Приложение использует камеру. Запись может осуществляться даже при выключенном экране с целью экономии батареи. Останавливайте запись после завершения использования телефона.
        """.trimIndent(),
        "ok" to "ОК",
        "room_id_title" to "ID Комнаты",
        "room_id_prompt" to "Введите ID комнаты для этой сессии:",
        "permissions_required" to "Требуются разрешения!",
        "camera_permissions_msg" to "Пожалуйста, предоставьте разрешения на камеру и аудио.",
        "starting_camera" to "Запуск сервиса камеры...",
        "debug_logs" to "Debug Логи:",
        "hide_logs" to "Скрыть логи",
        "show_logs" to "Показать логи",
        "simulate" to "Симуляция",
        "zoom" to "Зум",
        "start_camera" to "Включить камеру",
        "stop_camera" to "Выключить камеру",
        "camera_disabled" to "Камера выключена"
    )
}

@Composable
fun MainApp() {
    // 1. Fix: Use rememberSaveable for step to survive rotation
    var currentStep by rememberSaveable { mutableStateOf(AppStep.INTRO) }
    
    // Check system locale for default, also saveable
    val systemLocale = Locale.getDefault().language
    var isRussian by rememberSaveable { mutableStateOf(systemLocale == "ru") }

    when (currentStep) {
        AppStep.INTRO -> IntroScreen(
            isRussian = isRussian,
            onLanguageChange = { isRussian = it },
            onNext = { currentStep = AppStep.ROOM_ID }
        )
        AppStep.ROOM_ID -> RoomIdScreen(
            isRussian = isRussian,
            onNext = { currentStep = AppStep.CAMERA }
        )
        AppStep.CAMERA -> CameraScreen(
            isRussian = isRussian,
            // 2. Fix: If user enables camera inside CameraScreen, they might want to change room
            // But if they are just toggling, we keep them here.
            // Wait, the requirement says: "When turning on the camera, first show the room form".
            // So if they are in CameraScreen and 'Enable', we should redirect to RoomID.
            onEditRoom = { currentStep = AppStep.ROOM_ID }
        )
    }
}

@Composable
fun IntroScreen(
    isRussian: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("welcome", isRussian),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { onLanguageChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRussian) MaterialTheme.colorScheme.primary else Color.Gray
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Русский")
            }
            Button(
                onClick = { onLanguageChange(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRussian) MaterialTheme.colorScheme.primary else Color.Gray
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("English")
            }
        }

        Text(
            text = Strings.get("intro_text", isRussian),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(Strings.get("ok", isRussian))
        }
    }
}

@Composable
fun RoomIdScreen(
    isRussian: Boolean,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cameracontrol_prefs", Context.MODE_PRIVATE) }
    var roomId by remember { mutableStateOf(prefs.getString("room_id", "") ?: "") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("room_id_title", isRussian),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = Strings.get("room_id_prompt", isRussian),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = roomId,
            onValueChange = { roomId = it },
            label = { Text(Strings.get("room_id_title", isRussian)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        Button(
            onClick = {
                prefs.edit { putString("room_id", roomId) }
                onNext()
            },
            enabled = roomId.isEnabled(), // Blank check
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(Strings.get("ok", isRussian))
        }
    }
}

private fun String.isEnabled(): Boolean = this.isNotBlank()

@Composable
fun CameraScreen(
    isRussian: Boolean,
    onEditRoom: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cameracontrol_prefs", Context.MODE_PRIVATE) }

    // Toggle for Camera Service
    // We default to true because if they came from RoomID, they want to start.
    // If they toggle it off, they stay here safely.
    var isCameraEnabled by rememberSaveable { mutableStateOf(true) }

    // Permission State
    val hasPermissionsInitial = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    var hasPermissions by remember { mutableStateOf(hasPermissionsInitial) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grantedMap ->
            hasPermissions = grantedMap.values.all { it }
            if (!hasPermissions) {
                Toast.makeText(context, Strings.get("permissions_required", isRussian), Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        BufferManager.initialize(context)
        AppLogger.log("App Started. Checking Permissions...")
        if (!hasPermissions) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    var cameraService by remember { mutableStateOf<CameraForegroundService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    // Handle Service Binding based on isCameraEnabled and permissions
    DisposableEffect(hasPermissions, isCameraEnabled) {
        if (!hasPermissions || !isCameraEnabled) {
            
            // If explicit OFF, send stop command
            if (!isCameraEnabled && hasPermissions) {
                 val stopIntent = Intent(context, CameraForegroundService::class.java).apply {
                    action = CameraForegroundService.ACTION_STOP
                }
                context.startService(stopIntent) 
            }
            
            cameraService = null
            isBound = false
            return@DisposableEffect onDispose { }
        }

        // Start and Bind
        val intent = Intent(context, CameraForegroundService::class.java).apply {
            action = CameraForegroundService.ACTION_START
            // 3. Fix: Pass Room ID to Service so it can subscribe properly
            val rId = prefs.getString("room_id", null) 
            if (rId != null) {
                putExtra("room_id", rId)
            }
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

    LaunchedEffect(zoomLinear) {
        delay(60)
        recorder?.setLinearZoom(zoomLinear)
    }

    // Brief flash overlay whenever a WebSocket message is received
    LaunchedEffect(recorder) {
        recorder ?: return@LaunchedEffect
        NetworkClient.messageFlash.collectLatest {
            recorder.pulseTorch()
        }
    }
    
    LaunchedEffect(recorder) {
        recorder ?: return@LaunchedEffect
        NetworkClient.connectionStatus.collectLatest { isConnected ->
            recorder.setTorchEnabled(!isConnected)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isCameraEnabled && hasPermissions && recorder != null) {
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
        } else if (!isCameraEnabled) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Strings.get("camera_disabled", isRussian), color = Color.White)
            }
        } else if (!hasPermissions) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Strings.get("camera_permissions_msg", isRussian), color = Color.White)
            }
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Strings.get("starting_camera", isRussian), color = Color.White)
            }
        }
        
        // UI Controls (Overlay)
        // DEBUG LOGS OVERLAY
        var showLogs by remember { mutableStateOf(false) }

        if (showLogs) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(Strings.get("debug_logs", isRussian), color = Color.White, fontSize = 14.sp)
                LazyColumn(reverseLayout = false) {
                    items(AppLogger.logs) { log ->
                        Text(text = log, color = Color.Green, fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Log Build Time
        LaunchedEffect(Unit) {
            AppLogger.log("Build Time: " + BuildConfig.BUILD_TIME)
        }

        // Top compact actions
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactButton(
                text = if (showLogs) Strings.get("hide_logs", isRussian) else Strings.get("show_logs", isRussian),
                onClick = { showLogs = !showLogs }
            )

            CompactButton(
                text = Strings.get("simulate", isRussian),
                onClick = {
                    AppLogger.log("Manual Trigger Clicked")
                    NetworkClient.manualTrigger((System.currentTimeMillis() / 1000).toString())
                }
            )
        }

        // Right side zoom control to keep center clear
        if (isCameraEnabled) {
            val sliderInteraction = remember { MutableInteractionSource() }
            var isDragging by remember { mutableStateOf(false) }

            LaunchedEffect(sliderInteraction) {
                sliderInteraction.interactions.collect { interaction ->
                    when (interaction) {
                        is DragInteraction.Start -> isDragging = true
                        is DragInteraction.Stop, is DragInteraction.Cancel -> isDragging = false
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.9f)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isDragging) {
                    Text(
                        text = "${Strings.get("zoom", isRussian)} ${(zoomLinear * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .width(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = zoomLinear,
                        onValueChange = { value -> zoomLinear = value },
                        interactionSource = sliderInteraction,
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .verticalSlider()
                            .scale(1.35f)
                    )
                }
            }
        }

        // Centered toggle button keeps main view unobstructed
        CompactButton(
            text = if (isCameraEnabled) Strings.get("stop_camera", isRussian) else Strings.get("start_camera", isRussian),
            onClick = { 
                if (isCameraEnabled) {
                    isCameraEnabled = false 
                } else {
                    // Turning ON: Request Room Change first
                    onEditRoom()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCameraEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            fontSize = 15.sp,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun CompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
) {
    Button(
        onClick = onClick,
        colors = colors,
        contentPadding = contentPadding,
        modifier = modifier.defaultMinSize(minWidth = 0.dp)
    ) {
        Text(text, fontSize = fontSize)
    }
}

private fun Modifier.verticalSlider(): Modifier = composed {
    this
        .graphicsLayer {
            rotationZ = -90f
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
        }
        .layout { measurable, constraints ->
            // Force slider's unrotated width to match available height to lengthen the track
            val targetWidth = constraints.maxHeight
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = targetWidth,
                    maxWidth = targetWidth
                )
            )
            layout(placeable.height, placeable.width) {
                // Center after rotation to avoid drift
                placeable.place(
                    x = -(placeable.width - placeable.height) / 2,
                    y = (placeable.width - placeable.height) / 2
                )
            }
        }
}
