package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.camera.CameraManagerController
import com.example.model.AspectRatioSetting
import com.example.model.CameraMode
import com.example.model.CameraSettings
import com.example.model.FlashMode
import com.example.model.MediaItem
import com.example.model.TimerSetting
import com.example.storage.MediaStorageManager
import com.example.ui.components.CameraGrid
import com.example.ui.components.FocusReticle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecordingRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(
    onNavigateToEnhance: (String) -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var cameraSettings by remember { mutableStateOf(CameraSettings()) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var latestMediaItem by remember { mutableStateOf<MediaItem?>(null) }

    // Initialize CameraManagerController
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraController = remember {
        CameraManagerController(context, lifecycleOwner, previewView)
    }

    val isRecording by cameraController.isRecording.collectAsState()
    val recordingDurationSec by cameraController.recordingDurationSec.collectAsState()
    val zoomRatio by cameraController.zoomRatio.collectAsState()
    val exposureCompensation by cameraController.exposureCompensation.collectAsState()
    val exposureRange by cameraController.exposureRange.collectAsState()
    val is4kSupported by cameraController.is4kSupported.collectAsState()

    // Start camera
    DisposableEffect(Unit) {
        cameraController.startCamera {
            scope.launch {
                val mediaList = MediaStorageManager.queryAllMedia(context)
                if (mediaList.isNotEmpty()) {
                    latestMediaItem = mediaList.first()
                }
            }
        }
        onDispose {
            cameraController.shutdown()
        }
    }

    // Refresh latest media thumbnail
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            val mediaList = MediaStorageManager.queryAllMedia(context)
            if (mediaList.isNotEmpty()) {
                latestMediaItem = mediaList.first()
            }
        }
    }

    fun executeShutter() {
        if (cameraController.currentMode == CameraMode.VIDEO) {
            if (!isRecording) {
                cameraController.startVideoRecording(
                    onStarted = {
                        Toast.makeText(context, "4K UHD Recording Started", Toast.LENGTH_SHORT).show()
                    },
                    onFinished = { videoFile ->
                        scope.launch {
                            val savedUri = MediaStorageManager.saveVideoToGallery(context, videoFile, false)
                            if (savedUri != null) {
                                Toast.makeText(context, "4K Video Saved to Gallery", Toast.LENGTH_LONG).show()
                                val list = MediaStorageManager.queryAllMedia(context)
                                if (list.isNotEmpty()) latestMediaItem = list.first()
                            }
                        }
                    },
                    onError = { err ->
                        Toast.makeText(context, "Recording Error: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                cameraController.stopVideoRecording()
            }
        } else {
            cameraController.takePhoto(
                onSuccess = { photoFile ->
                    scope.launch {
                        // If auto-enhance is enabled, navigate to enhance screen
                        onNavigateToEnhance(photoFile.absolutePath)
                    }
                },
                onError = { err ->
                    Toast.makeText(context, "Capture Error: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun triggerCapture() {
        if (countdownRemaining > 0) return

        val timerSeconds = cameraSettings.timerSetting.seconds
        if (timerSeconds > 0) {
            scope.launch {
                countdownRemaining = timerSeconds
                while (countdownRemaining > 0) {
                    delay(1000)
                    countdownRemaining -= 1
                }
                executeShutter()
            }
        } else {
            executeShutter()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("camera_screen")
    ) {
        // 1. Live Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        focusPoint = offset
                        cameraController.focusOnPoint(offset.x, offset.y)
                    }
                }
        )

        // 2. Rule of thirds Grid Overlay
        if (cameraSettings.isGridEnabled) {
            CameraGrid()
        }

        // 3. Focus Reticle
        FocusReticle(
            focusPoint = focusPoint,
            onAnimationEnd = { focusPoint = null }
        )

        // 4. Timer Countdown Overlay
        AnimatedVisibility(
            visible = countdownRemaining > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownRemaining",
                    color = ProGold,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 5. Video Recording Status Banner
        if (isRecording) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alphaPulse by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alphaPulse"
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                    .border(1.dp, RecordingRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(RecordingRed.copy(alpha = alphaPulse), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val minutes = recordingDurationSec / 60
                val seconds = recordingDurationSec % 60
                val timeStr = String.format("%02d:%02d", minutes, seconds)
                Text(
                    text = "$timeStr • 4K UHD 60FPS",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 6. Top Bar Toolbar (Extends to top of screen with status bar gradient)
        TopCameraToolbar(
            modifier = Modifier.align(Alignment.TopCenter),
            currentFlash = cameraSettings.flashMode,
            onFlashToggle = {
                val nextFlash = when (cameraSettings.flashMode) {
                    FlashMode.OFF -> FlashMode.ON
                    FlashMode.ON -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.TORCH
                    FlashMode.TORCH -> FlashMode.OFF
                }
                cameraSettings = cameraSettings.copy(flashMode = nextFlash)
                cameraController.setFlashMode(nextFlash)
            },
            currentTimer = cameraSettings.timerSetting,
            onTimerToggle = {
                val nextTimer = when (cameraSettings.timerSetting) {
                    TimerSetting.OFF -> TimerSetting.SEC_3
                    TimerSetting.SEC_3 -> TimerSetting.SEC_10
                    TimerSetting.SEC_10 -> TimerSetting.OFF
                }
                cameraSettings = cameraSettings.copy(timerSetting = nextTimer)
            },
            currentAspectRatio = cameraSettings.aspectRatio,
            onAspectRatioToggle = {
                val nextRatio = when (cameraSettings.aspectRatio) {
                    AspectRatioSetting.RATIO_4_3 -> AspectRatioSetting.RATIO_16_9
                    AspectRatioSetting.RATIO_16_9 -> AspectRatioSetting.RATIO_1_1
                    AspectRatioSetting.RATIO_1_1 -> AspectRatioSetting.RATIO_4_3
                }
                cameraSettings = cameraSettings.copy(aspectRatio = nextRatio)
                cameraController.setAspectRatio(nextRatio)
            },
            isGridEnabled = cameraSettings.isGridEnabled,
            onGridToggle = {
                cameraSettings = cameraSettings.copy(isGridEnabled = !cameraSettings.isGridEnabled)
            },
            onSettingsClick = onNavigateToSettings
        )

        // 7. Pro Mode Manual Controls Slider (visible in PRO mode)
        if (cameraController.currentMode == CameraMode.PRO) {
            ProControlsOverlay(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                exposureValue = exposureCompensation,
                exposureRange = exposureRange,
                onExposureChange = { ev ->
                    cameraController.setExposureCompensation(ev)
                }
            )
        }

        // 8. Unified Bottom Controls Panel (Zoom -> Mode -> Shutter)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(top = 18.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Pills
            ZoomPillsBar(
                currentZoom = zoomRatio,
                onSelectZoom = { z -> cameraController.setZoom(z) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector Carousel
            CameraModeCarousel(
                selectedMode = cameraController.currentMode,
                onSelectMode = { mode ->
                    cameraController.setMode(mode)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Shutter & Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CharcoalSurface)
                        .border(1.5.dp, GraphiteBorder, CircleShape)
                        .clickable { onNavigateToGallery() }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestMediaItem != null) {
                        AsyncImage(
                            model = latestMediaItem!!.uri,
                            contentDescription = "Gallery Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Gallery",
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Master Shutter Button
                ShutterButton(
                    isRecording = isRecording,
                    isPhotoMode = cameraController.currentMode != CameraMode.VIDEO,
                    onClick = { triggerCapture() }
                )

                // Lens Switcher Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CharcoalSurface)
                        .border(1.5.dp, GraphiteBorder, CircleShape)
                        .clickable { cameraController.switchCamera() }
                        .testTag("switch_camera_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TopCameraToolbar(
    modifier: Modifier = Modifier,
    currentFlash: FlashMode,
    onFlashToggle: () -> Unit,
    currentTimer: TimerSetting,
    onTimerToggle: () -> Unit,
    currentAspectRatio: AspectRatioSetting,
    onAspectRatioToggle: () -> Unit,
    isGridEnabled: Boolean,
    onGridToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Black.copy(alpha = 0.85f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Flash Toggle
        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier.testTag("flash_toggle_button")
        ) {
            val icon = when (currentFlash) {
                FlashMode.OFF -> Icons.Default.FlashOff
                FlashMode.ON -> Icons.Default.FlashOn
                FlashMode.AUTO -> Icons.Default.FlashAuto
                FlashMode.TORCH -> Icons.Default.Highlight
            }
            Icon(
                imageVector = icon,
                contentDescription = "Flash: ${currentFlash.title}",
                tint = if (currentFlash != FlashMode.OFF) ProGold else PureWhite
            )
        }

        // Timer Toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onTimerToggle() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("timer_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = if (currentTimer != TimerSetting.OFF) ProGold else PureWhite,
                    modifier = Modifier.size(18.dp)
                )
                if (currentTimer != TimerSetting.OFF) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentTimer.label,
                        color = ProGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Aspect Ratio Toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAspectRatioToggle() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("aspect_ratio_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentAspectRatio.title,
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Grid Toggle
        IconButton(
            onClick = onGridToggle,
            modifier = Modifier.testTag("grid_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = "Grid",
                tint = if (isGridEnabled) ProGold else MutedSilver
            )
        }

        // Settings Shortcut
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = PureWhite
            )
        }
    }
}
}

@Composable
private fun ZoomPillsBar(
    modifier: Modifier = Modifier,
    currentZoom: Float,
    onSelectZoom: (Float) -> Unit
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val zoomLevels = listOf(1.0f, 2.0f, 5.0f)
        zoomLevels.forEach { z ->
            val isSelected = Math.abs(currentZoom - z) < 0.2f
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) ProGold else Color.Transparent)
                    .clickable { onSelectZoom(z) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("zoom_button_${z.toInt()}x"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${z.toInt()}x",
                    color = if (isSelected) ObsidianBlack else PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CameraModeCarousel(
    selectedMode: CameraMode,
    onSelectMode: (CameraMode) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("camera_mode_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.title,
                    color = if (isSelected) ProGold else MutedSilver,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(ProGold, CircleShape)
                    )
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(
    isRecording: Boolean,
    isPhotoMode: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1.0f, label = "shutterScale")

    Box(
        modifier = Modifier
            .size(78.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(4.dp, PureWhite, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(6.dp)
            .testTag("main_shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Red rounded square when recording
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(RecordingRed, RoundedCornerShape(6.dp))
            )
        } else {
            // White circular core for photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isPhotoMode) PureWhite else RecordingRed, CircleShape)
            )
        }
    }
}

@Composable
private fun ProControlsOverlay(
    modifier: Modifier = Modifier,
    exposureValue: Int,
    exposureRange: android.util.Range<Int>,
    onExposureChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EV: ${if (exposureValue > 0) "+$exposureValue" else "$exposureValue"}",
            color = ProGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Mini exposure slider
        Slider(
            value = exposureValue.toFloat(),
            onValueChange = { onExposureChange(it.toInt()) },
            valueRange = exposureRange.lower.toFloat()..exposureRange.upper.toFloat(),
            steps = (exposureRange.upper - exposureRange.lower),
            colors = SliderDefaults.colors(
                thumbColor = ProGold,
                activeTrackColor = ProGold,
                inactiveTrackColor = MutedSilver.copy(alpha = 0.3f)
            ),
            modifier = Modifier.width(120.dp)
        )
    }
}
