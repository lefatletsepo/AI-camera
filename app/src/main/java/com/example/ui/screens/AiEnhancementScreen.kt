package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiEnhancementEngine
import com.example.ai.EnhancementMetrics
import com.example.model.AiQuality
import com.example.storage.MediaStorageManager
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AiEnhancementScreen(
    rawPhotoPath: String,
    onBack: () -> Unit,
    onSavedSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var metrics by remember { mutableStateOf<EnhancementMetrics?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableIntStateOf(0) }
    var currentStage by remember { mutableStateOf("") }
    var intensity by remember { mutableFloatStateOf(0.85f) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }

    // Initial load and run
    LaunchedEffect(rawPhotoPath) {
        val file = File(rawPhotoPath)
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            originalBitmap = bmp
            if (bmp != null) {
                isProcessing = true
                scope.launch {
                    try {
                        val (resultBmp, resultMetrics) = AiEnhancementEngine.enhancePhotograph(
                            context = context,
                            inputBitmap = bmp,
                            intensity = intensity,
                            quality = AiQuality.BALANCED,
                            skinTonePreserve = true
                        ) { pct, stage ->
                            progressPercent = pct
                            currentStage = stage
                        }
                        enhancedBitmap = resultBmp
                        metrics = resultMetrics
                    } catch (e: Exception) {
                        Toast.makeText(context, "Enhancement failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isProcessing = false
                    }
                }
            }
        }
    }

    fun reprocess() {
        val bmp = originalBitmap ?: return
        isProcessing = true
        scope.launch {
            try {
                val (resultBmp, resultMetrics) = AiEnhancementEngine.enhancePhotograph(
                    context = context,
                    inputBitmap = bmp,
                    intensity = intensity,
                    quality = AiQuality.BALANCED,
                    skinTonePreserve = true
                ) { pct, stage ->
                    progressPercent = pct
                    currentStage = stage
                }
                enhancedBitmap = resultBmp
                metrics = resultMetrics
            } catch (e: Exception) {
                Toast.makeText(context, "Enhancement failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun saveEnhanced() {
        val bmp = enhancedBitmap ?: return
        scope.launch {
            val uri = MediaStorageManager.savePhotoToGallery(context, bmp, isEnhanced = true)
            if (uri != null) {
                savedUri = uri
                Toast.makeText(context, "Saved 4K Masterwork to Gallery", Toast.LENGTH_LONG).show()
                onSavedSuccessfully()
            } else {
                Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sharePhoto() {
        val uri = savedUri
        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share DARKSOULSED Photo"))
        } else {
            // Save first then share
            val bmp = enhancedBitmap ?: return
            scope.launch {
                val uriSaved = MediaStorageManager.savePhotoToGallery(context, bmp, isEnhanced = true)
                if (uriSaved != null) {
                    savedUri = uriSaved
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uriSaved)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share DARKSOULSED Photo"))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("ai_enhancement_screen")
    ) {
        // Main Content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }

                Text(
                    text = "AI COMPUTATIONAL LAB",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                IconButton(
                    onClick = { sharePhoto() },
                    modifier = Modifier.testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = PureWhite
                    )
                }
            }

            // Before / After Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ObsidianBlack)
            ) {
                if (originalBitmap != null) {
                    BeforeAfterSlider(
                        original = originalBitmap!!,
                        enhanced = enhancedBitmap
                    )
                }

                // Processing Progress Overlay
                if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ProGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$progressPercent%",
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentStage,
                            color = MutedSilver,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ProGold,
                            trackColor = CharcoalSurface
                        )
                    }
                }
            }

            // Bottom Controls Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurface)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                // Enhancement Metrics Bar
                if (metrics != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianBlack, RoundedCornerShape(10.dp))
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(label = "DETAIL", value = "+${metrics!!.detailBoostPercent}%")
                        MetricPill(label = "NOISE", value = "-${metrics!!.noiseReductionPercent}%")
                        MetricPill(label = "DYNAMIC", value = "+${String.format("%.1f", metrics!!.dynamicRangeGainEv)} EV")
                        MetricPill(label = "TONE", value = metrics!!.skinTonePurity)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Intensity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENHANCEMENT INTENSITY",
                        color = MutedSilver,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${(intensity * 100).toInt()}%",
                        color = ProGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = PureWhite,
                        activeTrackColor = PureWhite,
                        inactiveTrackColor = GraphiteBorder
                    ),
                    modifier = Modifier.testTag("intensity_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { reprocess() },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("reprocess_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RE-TUNE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { saveEnhanced() },
                        enabled = !isProcessing && enhancedBitmap != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("save_enhanced_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "SAVE 4K HDR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = MutedSilver,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = PureWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}
