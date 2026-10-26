package com.spectreflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.blender.LongExposureMode
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.AccentMint
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

@Composable
fun ModeSelector(
    selectedMode: LongExposureMode,
    detectedMode: LongExposureMode,
    onModeSelected: (LongExposureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x9914171A))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LongExposureMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val titleText = when (mode) {
                LongExposureMode.AUTO -> {
                    val detectedShort = when (detectedMode) {
                        LongExposureMode.LIGHT_TRAILS -> stringResource(R.string.mode_short_light_trails)
                        LongExposureMode.VANISH -> stringResource(R.string.mode_short_vanish)
                        else -> stringResource(R.string.mode_short_motion_blur)
                    }
                    if (isSelected) "${stringResource(R.string.mode_auto)} ($detectedShort)" else stringResource(R.string.mode_auto)
                }
                LongExposureMode.MOTION_BLUR -> stringResource(R.string.mode_motion_blur)
                LongExposureMode.LIGHT_TRAILS -> stringResource(R.string.mode_light_trails)
                LongExposureMode.VANISH -> stringResource(R.string.mode_vanish)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) AccentEmerald.copy(alpha = 0.2f) else Color.Transparent
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "Capture Mode: $titleText"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = titleText,
                    color = if (isSelected) AccentEmerald else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
