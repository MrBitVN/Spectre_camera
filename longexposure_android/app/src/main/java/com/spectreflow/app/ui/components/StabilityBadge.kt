package com.spectreflow.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.sensor.StabilityState
import com.spectreflow.app.ui.theme.AccentAmber
import com.spectreflow.app.ui.theme.AccentEmerald

@Composable
fun StabilityBadge(
    stability: StabilityState,
    modifier: Modifier = Modifier
) {
    val isStable = stability == StabilityState.STABLE
    val badgeColor by animateColorAsState(
        targetValue = if (isStable) AccentEmerald else AccentAmber,
        label = "stability_color"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x9914171A))
            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )

        Text(
            text = if (isStable) stringResource(R.string.stability_stable) else stringResource(R.string.stability_shaky),
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
