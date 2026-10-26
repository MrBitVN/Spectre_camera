package com.spectreflow.app.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.spectreflow.app.R
import com.spectreflow.app.blender.LongExposureMode
import com.spectreflow.app.sensor.StabilityState
import com.spectreflow.app.settings.CaptureDuration
import com.spectreflow.app.ui.components.DurationSelector
import com.spectreflow.app.ui.components.GridOverlay
import com.spectreflow.app.ui.components.LensSelector
import com.spectreflow.app.ui.components.ModeSelector
import com.spectreflow.app.ui.components.RingShutterButton
import com.spectreflow.app.ui.components.StabilityBadge
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

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
    isLimitedAccess: Boolean,
    showGrid: Boolean,
    isProUnlocked: Boolean,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onLensSelected: (Float) -> Unit,
    onFlipCamera: () -> Unit,
    onShutterPressStart: () -> Unit,
    onShutterPressRelease: () -> Unit,
    onShutterPressCancel: () -> Unit,
    onModeSelected: (LongExposureMode) -> Unit,
    onDurationSelected: (CaptureDuration) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        // CameraX Live Preview
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

        // Rule of thirds grid
        if (showGrid) {
            GridOverlay()
        }

        // Top Controls: App Name / Stability Badge / Settings Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = AccentEmerald,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    StabilityBadge(stability = stability)
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x9914171A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = TextPrimary
                    )
                }
            }

            // Limited Access Notification Banner
            if (isLimitedAccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC14171A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.limited_access_hint),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bottom Controls: Lens Selector, Modes, Durations, Shutter Button, Hint Labels
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lens Switching & Camera Flip
            LensSelector(
                supportedLenses = supportedLenses,
                currentZoom = currentZoom,
                isFrontCamera = isFrontCamera,
                onLensSelected = onLensSelected,
                onFlipCamera = onFlipCamera
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector (Auto, Motion Blur, Light Trails, Vanish)
            ModeSelector(
                selectedMode = selectedMode,
                detectedMode = detectedMode,
                onModeSelected = onModeSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Duration Selector (3s, 5s, 9s, 15s, 30s)
            DurationSelector(
                selectedDuration = selectedDuration,
                isProUnlocked = isProUnlocked,
                onDurationSelected = onDurationSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Shutter Instruction / Hint Banner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .height(28.dp)
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
                } else {
                    Text(
                        text = stringResource(R.string.hint_press_hold),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Press-and-Hold Shutter Button
            RingShutterButton(
                isCapturing = isCapturing,
                progress = captureProgress,
                elapsedSeconds = elapsedSeconds,
                onPressStart = onShutterPressStart,
                onPressRelease = onShutterPressRelease,
                onPressCancel = onShutterPressCancel
            )
        }
    }
}
