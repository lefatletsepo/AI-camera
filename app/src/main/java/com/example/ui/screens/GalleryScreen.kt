package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.storage.MediaStorageManager
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecordingRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onEnhanceVideo: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activeViewerItem by remember { mutableStateOf<MediaItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }

    val categories = listOf("All Media", "AI Enhanced", "Photos", "4K Videos")

    fun reload() {
        isLoading = true
        scope.launch {
            allMedia = MediaStorageManager.queryAllMedia(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    val filteredMedia = remember(allMedia, selectedTabIndex) {
        when (selectedTabIndex) {
            1 -> allMedia.filter { it.isEnhanced }
            2 -> allMedia.filter { !it.isVideo }
            3 -> allMedia.filter { it.isVideo }
            else -> allMedia
        }
    }

    fun shareItem(item: MediaItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/mp4" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    }

    fun deleteItem(item: MediaItem) {
        scope.launch {
            val success = MediaStorageManager.deleteMedia(context, item.uri)
            if (success) {
                Toast.makeText(context, "Deleted from device", Toast.LENGTH_SHORT).show()
                if (activeViewerItem?.id == item.id) {
                    activeViewerItem = null
                }
                reload()
            } else {
                Toast.makeText(context, "Unable to delete file", Toast.LENGTH_SHORT).show()
            }
            itemToDelete = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gallery_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }

                Text(
                    text = "DARKSOULSED GALLERY",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Box(modifier = Modifier.size(48.dp))
            }

            // Category Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = ObsidianBlack,
                contentColor = PureWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ProGold,
                        height = 2.dp
                    )
                },
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) ProGold else MutedSilver,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Grid Content
            if (isLoading) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ProGold)
                }
            } else if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            tint = GraphiteBorder,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No captured media found",
                            color = MutedSilver,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Take a photo or record 4K video to start",
                            color = MutedSilver.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        GalleryThumbnailItem(
                            item = item,
                            onClick = { activeViewerItem = item }
                        )
                    }
                }
            }
        }

        // Full Screen Media Viewer Modal
        if (activeViewerItem != null) {
            FullScreenMediaViewer(
                item = activeViewerItem!!,
                onClose = { activeViewerItem = null },
                onShare = { shareItem(activeViewerItem!!) },
                onDelete = { itemToDelete = activeViewerItem!! },
                onEnhanceVideo = { onEnhanceVideo(activeViewerItem!!) }
            )
        }

        // Delete Confirmation Dialog
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text(text = "Delete Media?", color = PureWhite) },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete '${itemToDelete!!.name}' from device storage?",
                        color = MutedSilver
                    )
                },
                containerColor = CharcoalSurface,
                confirmButton = {
                    Button(
                        onClick = { deleteItem(itemToDelete!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = RecordingRed)
                    ) {
                        Text(text = "DELETE", color = PureWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text(text = "CANCEL", color = PureWhite)
                    }
                }
            )
        }
    }
}

@Composable
private fun GalleryThumbnailItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalSurface)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // AI Enhanced Badge
        if (item.isEnhanced) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ProGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "AI", color = ProGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(text = "4K", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FullScreenMediaViewer(
    item: MediaItem,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEnhanceVideo: () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    val formattedDate = remember(item.dateAdded) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(item.dateAdded))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("fullscreen_media_viewer")
    ) {
        // Media Image or Video Frame
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Top Action Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (item.isEnhanced) "DARKSOULSED AI PRO" else "DARKSOULSED RAW",
                    color = if (item.isEnhanced) ProGold else PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = formattedDate, color = MutedSilver, fontSize = 10.sp)
            }

            IconButton(onClick = { showInfo = !showInfo }) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = PureWhite)
            }
        }

        // Info Drawer
        if (showInfo) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
                    .background(CharcoalSurfaceElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(text = "MEDIA TELEMETRY", color = ProGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "File: ${item.name}", color = PureWhite, fontSize = 11.sp)
                Text(text = "Type: ${if (item.isVideo) "4K HEVC Video" else "JPEG sRGB"}", color = MutedSilver, fontSize = 11.sp)
                Text(text = "Saved in: ${if (item.isVideo) "Movies/DARKSOULSED" else "Pictures/DARKSOULSED"}", color = MutedSilver, fontSize = 10.sp)
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share
            IconButton(onClick = onShare) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = PureWhite)
            }

            // Video Enhance Shortcut
            if (item.isVideo) {
                Button(
                    onClick = onEnhanceVideo,
                    colors = ButtonDefaults.buttonColors(containerColor = ProGold, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ENHANCE 4K", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Delete
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RecordingRed)
            }
        }
    }
}
