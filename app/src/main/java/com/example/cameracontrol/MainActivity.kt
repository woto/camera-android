package com.example.cameracontrol

import android.content.ComponentName
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
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
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import java.util.Locale

private const val TAG = "MainActivity"

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

@Composable
fun MainApp() {
    // 1. Fix: Use rememberSaveable for step to survive rotation
    var currentStep by rememberSaveable { mutableStateOf(AppStep.INTRO) }
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cameracontrol_prefs", Context.MODE_PRIVATE) }
    val initialLanguage = remember {
        AppLanguage.fromCode(prefs.getString("app_language", null))
            ?: AppStrings.defaultLanguage(Locale.getDefault())
    }
    var languageCode by rememberSaveable { mutableStateOf(initialLanguage.code) }
    val language = AppLanguage.fromCode(languageCode) ?: AppLanguage.EN

    when (currentStep) {
        AppStep.INTRO -> IntroScreen(
            language = language,
            onLanguageChange = { newLanguage ->
                languageCode = newLanguage.code
                prefs.edit { putString("app_language", newLanguage.code) }
            },
            onNext = { currentStep = AppStep.ROOM_ID }
        )
        AppStep.ROOM_ID -> RoomIdScreen(
            language = language,
            onNext = { currentStep = AppStep.CAMERA }
        )
        AppStep.CAMERA -> CameraScreen(
            language = language,
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
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onNext: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.volleycam_icon),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = AppStrings.get("welcome", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = AppStrings.get("website_label", language),
                fontSize = 12.sp,
                color = Color.Gray
            )
            TextButton(
                onClick = { uriHandler.openUri("https://volleycam.com") },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "volleycam.com",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = AppStrings.get("privacy_disclaimer", language),
                fontSize = 12.sp,
                color = Color.Gray
            )
            TextButton(
                onClick = { uriHandler.openUri("https://volleycam.com/privacy") },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = AppStrings.get("privacy_link", language),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(AppStrings.get("ok", language))
        }
    }
}


@Composable
fun RoomIdScreen(
    language: AppLanguage,
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
            text = AppStrings.get("room_id_title", language),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = AppStrings.get("room_id_prompt", language),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = roomId,
            onValueChange = { roomId = it },
            label = { Text(AppStrings.get("room_id_title", language)) },
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
            Text(AppStrings.get("ok", language))
        }
    }
}

private fun String.isEnabled(): Boolean = this.isNotBlank()

@Composable
fun CameraScreen(
    language: AppLanguage,
    onEditRoom: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("cameracontrol_prefs", Context.MODE_PRIVATE) }

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
                Toast.makeText(context, AppStrings.get("permissions_required", language), Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        BufferManager.initialize(context)
        AppLogger.log(TAG, "App Started. Checking Permissions...")
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
    var wasBound by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var wasForegroundRecording by remember { mutableStateOf(false) }
    val isForegroundRecording by CameraForegroundService.foregroundState.collectAsState()

    // Handle Service Binding based on isCameraEnabled and permissions
    DisposableEffect(hasPermissions) {
        if (!hasPermissions) {
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
                if (isBound) {
                    wasBound = true
                }
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
    var orientationDebug by remember { mutableStateOf<String?>(null) }
    var lastAttachedRotation by remember { mutableStateOf<Int?>(null) }
    var lastSurfaceProviderId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(isBound, hasPermissions) {
        if (isBound) {
            wasBound = true
        } else if (wasBound && hasPermissions) {
            onEditRoom()
        }
    }

    LaunchedEffect(isForegroundRecording, hasPermissions) {
        if (isForegroundRecording) {
            wasForegroundRecording = true
        } else if (wasForegroundRecording && hasPermissions && !isExiting) {
            onEditRoom()
        }
    }

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

    LaunchedEffect(recorder) {
        while (isActive) {
            orientationDebug = recorder?.getOrientationDebugString()
            delay(250)
        }
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (hasPermissions && recorder != null) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        val rotation = this.display?.rotation
                        val providerId = System.identityHashCode(this.surfaceProvider)
                        lastSurfaceProviderId = providerId
                        lastAttachedRotation = rotation
                        recorder.attachPreview(this.surfaceProvider, rotation)
                    }
                },
                update = { view ->
                    val rotation = view.display?.rotation
                    val providerId = System.identityHashCode(view.surfaceProvider)
                    if (rotation != lastAttachedRotation || providerId != lastSurfaceProviderId) {
                        lastSurfaceProviderId = providerId
                        lastAttachedRotation = rotation
                        recorder.attachPreview(view.surfaceProvider, rotation)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!hasPermissions) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("camera_permissions_msg", language), color = Color.White)
            }
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(AppStrings.get("starting_camera", language), color = Color.White)
            }
        }

        if (orientationDebug != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 56.dp, end = 10.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(orientationDebug!!, color = Color.White, fontSize = 12.sp)
            }
        }

        // Log Build Time
        LaunchedEffect(Unit) {
            AppLogger.log(TAG, "Build Time: " + BuildConfig.BUILD_TIME)
        }

        val edgePadding = 16.dp
        val exitWrapperSize = 56.dp
        val exitIconSize = 26.dp
        val sliderWrapperWidth = exitWrapperSize
        val sliderWrapperHeight = 260.dp
        val sliderCornerRadius = 18.dp
        val sliderVerticalPadding = 28.dp
        val sliderHorizontalPadding = 10.dp
        val sliderScale = 1.2f
        val hintSlotWidth = 68.dp
        val hintToSliderSpacing = 10.dp
        val totalControlWidth = sliderWrapperWidth + hintSlotWidth + hintToSliderSpacing
        val sliderOffsetY = (
            edgePadding
                + exitWrapperSize
                + ((maxHeight - edgePadding - exitWrapperSize - sliderWrapperHeight) / 2)
        ).coerceAtLeast(0.dp)

        val sliderInteraction = remember { MutableInteractionSource() }
        var isDragging by remember { mutableStateOf(false) }
        var labelHeightPx by remember { mutableStateOf(0) }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = edgePadding, top = edgePadding)
        ) {
            Box(
                modifier = Modifier
                    .size(exitWrapperSize)
                    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier.fillMaxSize(),
                    onClick = {
                        isExiting = true
                        val stopIntent = Intent(context, CameraForegroundService::class.java).apply {
                            action = CameraForegroundService.ACTION_STOP
                        }
                        context.startService(stopIntent)
                        activity?.finishAndRemoveTask() ?: activity?.finish()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = AppStrings.get("exit", language),
                        tint = Color.White,
                        modifier = Modifier.size(exitIconSize)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = edgePadding)
                .offset(y = sliderOffsetY)
        ) {
            if (recorder != null) {
                LaunchedEffect(sliderInteraction) {
                    sliderInteraction.interactions.collect { interaction ->
                        when (interaction) {
                            is DragInteraction.Start -> isDragging = true
                            is DragInteraction.Stop, is DragInteraction.Cancel -> isDragging = false
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.width(totalControlWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(hintSlotWidth)
                        .height(sliderWrapperHeight)
                        .padding(vertical = sliderVerticalPadding),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (recorder != null) {
                        val labelHeight = with(LocalDensity.current) { labelHeightPx.toDp() }
                        val sliderAreaHeight = sliderWrapperHeight - (sliderVerticalPadding * 2)
                        val scaledRange = sliderAreaHeight * sliderScale
                        val extraOffset = (scaledRange - sliderAreaHeight) / 2
                        val maxOffset = (scaledRange - labelHeight).coerceAtLeast(0.dp)
                        val labelOffset = (maxOffset * (1f - zoomLinear)) - extraOffset - 4.dp

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isDragging,
                            enter = fadeIn(tween(140)) + scaleIn(
                                tween(140),
                                initialScale = 0.96f
                            ),
                            exit = fadeOut(tween(140)) + scaleOut(
                                tween(140),
                                targetScale = 0.96f
                            )
                        ) {
                            Text(
                                text = "${(zoomLinear * 100).roundToInt()}%",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .offset(y = labelOffset)
                                    .background(
                                        Color.Black.copy(alpha = 0.35f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .onSizeChanged { size -> labelHeightPx = size.height }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(hintToSliderSpacing))

                Box(
                    modifier = Modifier
                        .width(sliderWrapperWidth)
                        .height(sliderWrapperHeight)
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(sliderCornerRadius))
                        .clip(RoundedCornerShape(sliderCornerRadius))
                        .padding(
                            horizontal = sliderHorizontalPadding,
                            vertical = sliderVerticalPadding
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (recorder != null) {
                            Slider(
                                value = zoomLinear,
                                onValueChange = { value -> zoomLinear = value },
                                interactionSource = sliderInteraction,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .verticalSlider()
                                    .scale(sliderScale)
                            )
                        }
                    }
            }
        }

        // Centered simulate button keeps main view unobstructed
        CompactButton(
            text = AppStrings.get("simulate_save", language),
            onClick = { 
                if (recorder == null) {
                    onEditRoom()
                } else {
                    AppLogger.log(TAG, "Trigger POST Clicked")
                    val roomId = prefs.getString("room_id", null)
                    NetworkClient.sendTrigger(roomId)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
