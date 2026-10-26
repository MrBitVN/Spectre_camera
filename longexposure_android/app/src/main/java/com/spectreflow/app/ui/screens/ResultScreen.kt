package com.spectreflow.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.AccentMint
import com.spectreflow.app.ui.theme.BgCard
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.BorderSubtle
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

@Composable
fun ResultScreen(
    resultBitmap: Bitmap,
    keyframes: List<Bitmap>,
    hasCompanionVideo: Boolean,
    onSavePhoto: (Bitmap) -> Unit,
    onSharePhoto: (Bitmap) -> Unit,
    onShareVideo: () -> Unit,
    onExportWallpaper: (() -> Unit)? = null,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // -1 represents the composite long exposure photo; 0..N-1 represents specific burst keyframes
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var activeBitmap by remember(resultBitmap) { mutableStateOf(resultBitmap) }

    val displayedBitmap = if (selectedIndex == -1 || selectedIndex >= keyframes.size) {
        activeBitmap
    } else {
        keyframes[selectedIndex]
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        // High quality preview
        Image(
            bitmap = displayedBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.result_title),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Top Status Badge (Composite vs Frame X)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC14171A))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (selectedIndex == -1) {
                    stringResource(R.string.label_composite_result)
                } else {
                    stringResource(R.string.label_frame_index, selectedIndex + 1, keyframes.size)
                },
                color = AccentEmerald,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Controls: Filmstrip Scrubber & Action Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xE60C0E10))
                .padding(vertical = 14.dp)
        ) {
            // Frame Scrubber (if keyframes available)
            if (keyframes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_scrub_frames),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        if (selectedIndex != -1) {
                            Button(
                                onClick = {
                                    activeBitmap = keyframes[selectedIndex]
                                    selectedIndex = -1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentMint),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.action_pick_as_cover),
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.label_composite_active),
                                color = AccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Filmstrip Thumbnails
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // First item: Composite Result
                        item {
                            Box(
                                modifier = Modifier
                                    .size(48.dp, 64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedIndex == -1) AccentEmerald else BgCard)
                                    .clickable { selectedIndex = -1 }
                                    .padding(2.dp)
                            ) {
                                Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.label_composite_result),
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Burst Keyframes
                        itemsIndexed(keyframes) { index, frame ->
                            val isCurrent = selectedIndex == index
                            Box(
                                modifier = Modifier
                                    .size(48.dp, 64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) AccentEmerald else Color.Transparent)
                                    .clickable { selectedIndex = index }
                                    .padding(if (isCurrent) 2.dp else 0.dp)
                            ) {
                                Image(
                                    bitmap = frame.asImageBitmap(),
                                    contentDescription = "Frame ${index + 1}",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Buttons: Discard, Share Video (if available), Share Photo, Save Photo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Discard
                OutlinedButton(
                    onClick = onDiscard,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = " " + stringResource(R.string.action_discard),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Share Video (Companion MP4)
                if (hasCompanionVideo) {
                    OutlinedButton(
                        onClick = onShareVideo,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentMint),
                        border = BorderStroke(1.dp, AccentMint.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentMint)
                        Text(
                            text = " " + stringResource(R.string.action_share_video),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Export Looping Wallpaper
                if (onExportWallpaper != null && keyframes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onExportWallpaper,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentMint),
                        border = BorderStroke(1.dp, AccentMint.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentMint)
                        Text(
                            text = " " + stringResource(R.string.action_export_wallpaper),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Share Photo
                OutlinedButton(
                    onClick = { onSharePhoto(displayedBitmap) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = " " + stringResource(R.string.action_share),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Save Photo
                Button(
                    onClick = { onSavePhoto(displayedBitmap) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = BgObsidian
                    ),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = " " + stringResource(R.string.action_save),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
