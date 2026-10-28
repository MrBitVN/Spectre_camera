package com.spectreflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.blender.LongExposureMode
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.BgCard
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Top bar matching reference layout:
 * - Left: Mode label "AUTO" with light-bulb icon. Tap opens mode picker modal.
 * - Center-Right: Tiny green status dot when camera is active.
 * - Right: Small sun icon with auto-hiding exposure compensation slider (3s timeout).
 */
@Composable
fun TopBarControls(
    selectedMode: LongExposureMode,
    detectedMode: LongExposureMode,
    isCameraActive: Boolean,
    exposureCompensation: Float,
    onExposureChanged: (Float) -> Unit,
    onModeSelected: (LongExposureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showModeMenu by remember { mutableStateOf(false) }
    var showExposureSlider by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-hide exposure compensation slider after 3 seconds of inactivity
    LaunchedEffect(showExposureSlider, lastInteractionTime) {
        if (showExposureSlider) {
            delay(3000)
            showExposureSlider = false
        }
    }

    // Status dot pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_dot_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Label Button: "AUTO 💡" (or detected mode in Auto)
            Box {
                val modeLabel = if (selectedMode == LongExposureMode.AUTO) {
                    val detectedShort = when (detectedMode) {
                        LongExposureMode.MOTION_BLUR -> "BLUR"
                        LongExposureMode.LIGHT_TRAILS -> "TRAILS"
                        LongExposureMode.VANISH -> "VANISH"
                        else -> "AUTO"
                    }
                    "AUTO"
                } else {
                    when (selectedMode) {
                        LongExposureMode.MOTION_BLUR -> "MOTION BLUR"
                        LongExposureMode.LIGHT_TRAILS -> "LIGHT TRAILS"
                        LongExposureMode.VANISH -> "VANISH"
                        else -> "AUTO"
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showModeMenu = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = modeLabel,
                        color = AccentEmerald,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = stringResource(R.string.cd_capture_mode, modeLabel),
                        tint = AccentEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Mode Picker Dropdown Menu
                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    modifier = Modifier
                        .background(BgCard)
                        .border(1.dp, Color(0xFF263238), RoundedCornerShape(12.dp))
                ) {
                    LongExposureMode.entries.forEach { mode ->
                        val isCurrent = mode == selectedMode
                        val nameStr = when (mode) {
                            LongExposureMode.AUTO -> stringResource(R.string.mode_auto)
                            LongExposureMode.MOTION_BLUR -> stringResource(R.string.mode_motion_blur)
                            LongExposureMode.LIGHT_TRAILS -> stringResource(R.string.mode_light_trails)
                            LongExposureMode.VANISH -> stringResource(R.string.mode_vanish)
                        }
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = nameStr,
                                        color = if (isCurrent) AccentEmerald else TextPrimary,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    if (mode == LongExposureMode.AUTO) {
                                        Text(
                                            text = "Auto scene analysis",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onModeSelected(mode)
                                showModeMenu = false
                            }
                        )
                    }
                }
            }

            // Right Cluster: Active Camera Dot & Sun Exposure Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tiny green camera active status dot
                if (isCameraActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = dotAlpha))
                    )
                }

                // Small Sun icon (Exposure Compensation toggle)
                IconButton(
                    onClick = {
                        showExposureSlider = !showExposureSlider
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = stringResource(R.string.cd_exposure_icon),
                        tint = if (showExposureSlider) AccentEmerald else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Auto-hiding horizontal Exposure Compensation Slider
        AnimatedVisibility(
            visible = showExposureSlider,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD914171A))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%+.1f EV", exposureCompensation),
                    color = AccentEmerald,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(55.dp)
                )
                Slider(
                    value = exposureCompensation,
                    onValueChange = {
                        lastInteractionTime = System.currentTimeMillis()
                        onExposureChanged(it)
                    },
                    valueRange = -2.0f..2.0f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentEmerald,
                        activeTrackColor = AccentEmerald,
                        inactiveTrackColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
