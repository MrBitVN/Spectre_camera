package com.spectreflow.app.ui.screens

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.spectreflow.app.R
import com.spectreflow.app.blender.LongExposureMode
import com.spectreflow.app.sensor.StabilityState
import com.spectreflow.app.settings.CaptureDuration
import com.spectreflow.app.ui.components.FloatingZoomPill
import com.spectreflow.app.ui.components.GridOverlay
import com.spectreflow.app.ui.components.RingShutterButton
import com.spectreflow.app.ui.components.RotaryDurationDial
import com.spectreflow.app.ui.components.StabilityIndicator
import com.spectreflow.app.ui.components.TopBarControls
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

/**
 * Main Camera Screen matching reference layout:
 *
 * 1. Top Bar:
 *    - Mode label "AUTO 💡", tap opens mode picker. In Auto, shows detected mode.
 *    - Active green camera status dot.
 *    - Sun brightness icon with auto-hiding exposure compensation slider.
 *
 * 2. Preview Area (~65% height, rounded corners):
 *    - Live CameraX preview with optional GridOverlay.
 *    - Floating zoom pill at bottom center ("1x", tap cycles supported lenses).
 *
 * 3. Control Row below preview:
 *    - Flip camera button on left.
 *    - Stability indicator [ 📱 ] in center (STABLE green / SHAKY amber).
 *    - Settings gear button on right.
 *
 * 4. Bottom Row:
 *    - Rounded-square thumbnail of last saved photo on left (opens result viewer).
 *    - Two concentric green rings shutter button in center (press & hold, pulse & arc).
 *    - Rotary duration dial in bottom-right corner (3, 5, 9, 15, 30s).
 */
@Composable
fun CameraScreen(
    stability: StabilityState,
    isCapturing: Boolean,
    captureProgress: Float,
    elapsedSeconds: Float,
    hintMessage: String?,
    selectedMode: LongExposureMode,
    detectedMode: LongExposureMode,
    selectedDuration: CaptureDuration,
    supportedLenses: List<Float>,
    currentZoom: Float,
    isFrontCamera: Boolean,
    showGrid: Boolean,
    isProUnlocked: Boolean,
    exposureCompensation: Float,
    lastSavedThumbnail: Bitmap?,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onLensSelected: (Float) -> Unit,
    onFlipCamera: () -> Unit,
    onShutterPressStart: () -> Unit,
    onShutterPressRelease: () -> Unit,
    onShutterPressCancel: () -> Unit,
    onModeSelected: (LongExposureMode) -> Unit,
    onDurationSelected: (CaptureDuration) -> Unit,
    onExposureChanged: (Float) -> Unit,
    onOpenThumbnailViewer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // === 1. TOP BAR ===
            TopBarControls(
                selectedMode = selectedMode,
                detectedMode = detectedMode,
                isCameraActive = true,
                exposureCompensation = exposureCompensation,
                onExposureChanged = onExposureChanged,
                onModeSelected = onModeSelected,
                modifier = Modifier.padding(top = 28.dp)
            )

            // === 2. PREVIEW VIEWPORT (~65% height, rounded corners) ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.66f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
            ) {
                // Live Camera View
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            onPreviewViewCreated(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Optional Rule-of-Thirds Grid
                if (showGrid) {
                    GridOverlay()
                }

                // Floating Zoom Pill at bottom center of preview
                FloatingZoomPill(
                    currentZoom = currentZoom,
                    supportedLenses = supportedLenses,
                    onZoomSelected = onLensSelected,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // === 3. CONTROL ROW (Flip | Stability [📱] | Settings Gear) ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip Camera Button
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.cd_flip_camera),
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Stability Indicator [ 📱 ]
                StabilityIndicator(stability = stability)

                // Settings Gear Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Interactive Shutter / Hint Banner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 24.dp)
            ) {
                if (hintMessage != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xD9E65100))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = hintMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xAA14171A))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hint_release_to_capture, selectedDuration.seconds),
                            color = AccentEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // === 4. BOTTOM CAPTURE STRIP (Thumbnail | Concentric Shutter | Rotary Dial) ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, start = 20.dp, end = 0.dp)
            ) {
                // Left: Last Photo Thumbnail
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E2228))
                        .border(1.5.dp, Color(0x44FFFFFF), RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenThumbnailViewer)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Last long exposure result"
                        }
                ) {
                    if (lastSavedThumbnail != null) {
                        Image(
                            bitmap = lastSavedThumbnail.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_recent_thumbnail),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = stringResource(R.string.no_photos_yet),
                            tint = Color(0xFF546E7A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center: Two Concentric Green Rings Shutter Button
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    RingShutterButton(
                        isCapturing = isCapturing,
                        progress = captureProgress,
                        elapsedSeconds = elapsedSeconds,
                        onPressStart = onShutterPressStart,
                        onPressRelease = onShutterPressRelease,
                        onPressCancel = onShutterPressCancel
                    )
                }

                // Right: Rotary Duration Dial in bottom-right corner
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    RotaryDurationDial(
                        selectedDuration = selectedDuration,
                        isProUnlocked = isProUnlocked,
                        onDurationSelected = onDurationSelected,
                        onLockedTapped = {
                            // Show pro required hint or trigger settings
                        }
                    )
                }
            }
        }
    }
}
