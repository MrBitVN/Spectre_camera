package com.spectreflow.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import java.util.Locale

/**
 * Floating zoom pill centered at the bottom of the camera preview.
 * Tapping cycles through the device's supported lenses (e.g. 0.5x, 1x, 2x, 3x).
 */
@Composable
fun FloatingZoomPill(
    currentZoom: Float,
    supportedLenses: List<Float>,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val zoomText = if (currentZoom == currentZoom.toInt().toFloat()) {
        "${currentZoom.toInt()}x"
    } else {
        String.format(Locale.US, "%.1fx", currentZoom)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x80000000))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = "Camera zoom $zoomText. Tap to switch lens"
            }
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val currentIndex = supportedLenses.indexOfFirst { kotlin.math.abs(it - currentZoom) < 0.1f }
                val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % supportedLenses.size else 0
                val targetZoom = supportedLenses.getOrElse(nextIndex) { 1.0f }
                onZoomSelected(targetZoom)
            }
    ) {
        Text(
            text = zoomText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
