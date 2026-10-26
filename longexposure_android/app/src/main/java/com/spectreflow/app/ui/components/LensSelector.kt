package com.spectreflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.TextPrimary
import com.spectreflow.app.ui.theme.TextSecondary

@Composable
fun LensSelector(
    supportedLenses: List<Float>,
    currentZoom: Float,
    isFrontCamera: Boolean,
    onLensSelected: (Float) -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x9914171A))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isFrontCamera) {
            supportedLenses.forEach { lensRatio ->
                val isSelected = kotlin.math.abs(currentZoom - lensRatio) < 0.15f
                val label = if (lensRatio < 1.0f) "0.5x" else "${lensRatio.toInt()}x"

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AccentEmerald else Color.Transparent)
                        .clickable { onLensSelected(lensRatio) }
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = "Zoom level $label"
                        }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Camera flip button
        IconButton(
            onClick = onFlipCamera,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = "Flip camera"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = null,
                tint = if (isFrontCamera) AccentEmerald else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
