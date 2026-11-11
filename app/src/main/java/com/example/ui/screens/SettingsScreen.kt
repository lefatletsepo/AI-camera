package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiQuality
import com.example.model.CameraSettings
import com.example.storage.MediaStorageManager
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.PureWhite

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(CameraSettings()) }
    var cacheBytes by remember { mutableLongStateOf(MediaStorageManager.getCacheSizeBytes(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 1: Camera Hardware
            SettingsHeader(title = "CAMERA HARDWARE & CAPTURE")

            SettingsSwitchItem(
                title = "4K UHD Capture Mode",
                subtitle = "Enables 3840x2160 recording when sensor supports it",
                checked = settings.is4kVideoEnabled,
                onCheckedChange = { settings = settings.copy(is4kVideoEnabled = it) }
            )

            SettingsSwitchItem(
                title = "60 FPS High Frame Rate",
                subtitle = "Fluid cinema recording where supported",
                checked = settings.fps60Enabled,
                onCheckedChange = { settings = settings.copy(fps60Enabled = it) }
            )

            SettingsSwitchItem(
                title = "Rule of Thirds Grid",
                subtitle = "Overlay composition guidelines on preview",
                checked = settings.isGridEnabled,
                onCheckedChange = { settings = settings.copy(isGridEnabled = it) }
            )

            SettingsSwitchItem(
                title = "Shutter Sound",
                subtitle = "Play tactile optical shutter audio on capture",
                checked = settings.shutterSoundEnabled,
                onCheckedChange = { settings = settings.copy(shutterSoundEnabled = it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: AI Neural Engine
            SettingsHeader(title = "AI NEURAL ENHANCEMENT ENGINE")

            SettingsSwitchItem(
                title = "Automatic AI Enhancement",
                subtitle = "Instantly launch AI computational lab after capturing photos",
                checked = settings.autoAiEnhance,
                onCheckedChange = { settings = settings.copy(autoAiEnhance = it) }
            )

            SettingsSwitchItem(
                title = "Skin Tone & Melanin Protection",
                subtitle = "Preserves natural skin texture without artificial smoothing or reddish casts",
                checked = settings.skinTonePreservation,
                onCheckedChange = { settings = settings.copy(skinTonePreservation = it) }
            )

            // AI Quality Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(CharcoalSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(text = "Processing Quality Profile", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiQuality.values().forEach { q ->
                        val isSelected = settings.aiQuality == q
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ProGold else CharcoalSurfaceElevated)
                                .clickable { settings = settings.copy(aiQuality = q) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q.name,
                                color = if (isSelected) ObsidianBlack else PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Storage & Maintenance
            SettingsHeader(title = "DEVICE STORAGE & CACHE")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(CharcoalSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Temporary Cache Usage", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val mb = cacheBytes / (1024f * 1024f)
                    Text(text = "${String.format("%.2f", mb)} MB stored in app cache", color = MutedSilver, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val freed = MediaStorageManager.clearTempCache(context)
                        cacheBytes = MediaStorageManager.getCacheSizeBytes(context)
                        Toast.makeText(context, "Cleared temporary files", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalSurfaceElevated,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "CLEAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: About
            SettingsHeader(title = "ABOUT APPLICATION")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(CharcoalSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToAbout() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "DARKSOULSED® AI CAMERA", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Version 1.0.0 PRO • Tap for details & signature", color = ProGold, fontSize = 11.sp)
                }
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ProGold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = MutedSilver,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(CharcoalSurface, RoundedCornerShape(12.dp))
            .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = MutedSilver, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = ProGold,
                uncheckedThumbColor = MutedSilver,
                uncheckedTrackColor = GraphiteBorder
            )
        )
    }
}
