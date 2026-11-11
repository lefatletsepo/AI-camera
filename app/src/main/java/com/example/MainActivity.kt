package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.model.MediaItem
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiEnhancementScreen
import com.example.ui.screens.AiVideoEnhancementScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DarkSoulsCameraTheme
import com.example.ui.theme.ObsidianBlack

sealed class AppScreen {
    object Splash : AppScreen()
    object Onboarding : AppScreen()
    object Camera : AppScreen()
    data class EnhancePhoto(val photoPath: String) : AppScreen()
    data class EnhanceVideo(val videoItem: MediaItem) : AppScreen()
    object Gallery : AppScreen()
    object Settings : AppScreen()
    object About : AppScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DarkSoulsCameraTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBlack)
                ) {
                    DarkSoulsApp(
                        hasCameraPermission = hasRequiredPermissions()
                    )
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun DarkSoulsApp(hasCameraPermission: Boolean) {
    var currentScreen by remember {
        mutableStateOf<AppScreen>(AppScreen.Splash)
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is AppScreen.Splash -> {
                SplashScreen(
                    onFinished = {
                        currentScreen = if (hasCameraPermission) {
                            AppScreen.Camera
                        } else {
                            AppScreen.Onboarding
                        }
                    }
                )
            }

            is AppScreen.Onboarding -> {
                OnboardingScreen(
                    onPermissionsGranted = {
                        currentScreen = AppScreen.Camera
                    }
                )
            }

            is AppScreen.Camera -> {
                CameraScreen(
                    onNavigateToEnhance = { path ->
                        currentScreen = AppScreen.EnhancePhoto(path)
                    },
                    onNavigateToGallery = {
                        currentScreen = AppScreen.Gallery
                    },
                    onNavigateToSettings = {
                        currentScreen = AppScreen.Settings
                    }
                )
            }

            is AppScreen.EnhancePhoto -> {
                BackHandler {
                    currentScreen = AppScreen.Camera
                }
                AiEnhancementScreen(
                    rawPhotoPath = screen.photoPath,
                    onBack = { currentScreen = AppScreen.Camera },
                    onSavedSuccessfully = { currentScreen = AppScreen.Gallery }
                )
            }

            is AppScreen.EnhanceVideo -> {
                BackHandler {
                    currentScreen = AppScreen.Gallery
                }
                AiVideoEnhancementScreen(
                    videoItem = screen.videoItem,
                    onBack = { currentScreen = AppScreen.Gallery },
                    onCompleted = { currentScreen = AppScreen.Gallery }
                )
            }

            is AppScreen.Gallery -> {
                BackHandler {
                    currentScreen = AppScreen.Camera
                }
                GalleryScreen(
                    onBack = { currentScreen = AppScreen.Camera },
                    onEnhanceVideo = { item ->
                        currentScreen = AppScreen.EnhanceVideo(item)
                    }
                )
            }

            is AppScreen.Settings -> {
                BackHandler {
                    currentScreen = AppScreen.Camera
                }
                SettingsScreen(
                    onBack = { currentScreen = AppScreen.Camera },
                    onNavigateToAbout = { currentScreen = AppScreen.About }
                )
            }

            is AppScreen.About -> {
                BackHandler {
                    currentScreen = AppScreen.Settings
                }
                AboutScreen(
                    onBack = { currentScreen = AppScreen.Settings }
                )
            }
        }
    }
}
