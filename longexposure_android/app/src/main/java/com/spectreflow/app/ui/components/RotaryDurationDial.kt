package com.spectreflow.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.settings.CaptureDuration
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rotary Duration Dial anchored in the bottom-right corner.
 *
 * Renders a quarter-circle arc with radial tick marks for exposure durations (3, 5, 9, 15, 30s).
 * Selected value is rendered large in neon emerald. Locked values display a lock indicator.
 * Supports swiping along the dial arc and direct tapping with tactile haptic feedback.
 */
@Composable
fun RotaryDurationDial(
    selectedDuration: CaptureDuration,
    isProUnlocked: Boolean,
    onDurationSelected: (CaptureDuration) -> Unit,
    onLockedTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val durations = remember {
        listOf(
            CaptureDuration.SEC_3,
            CaptureDuration.SEC_5,
            CaptureDuration.SEC_9,
            CaptureDuration.SEC_15,
            CaptureDuration.SEC_30
        )
    }

    // Arc geometry: Quarter circle in bottom-right corner
    // Angles in degrees from bottom-right origin (0 = East, 90 = South, 180 = West, 270 = North)
    // Dial spans from 185 deg (left/west) to 265 deg (top/north)
    val angleList = remember { listOf(190f, 208f, 226f, 244f, 262f) }

    fun selectByIndex(index: Int) {
        if (index !in durations.indices) return
        val target = durations[index]
        if (target != selectedDuration) {
            if (target.isProOnly && !isProUnlocked) {
                onLockedTapped()
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onDurationSelected(target)
            }
        }
    }

    fun findNearestIndex(angleDeg: Float): Int {
        var minDiff = Float.MAX_VALUE
        var bestIndex = 0
        for (i in angleList.indices) {
            val diff = kotlin.math.abs(angleDeg - angleList[i])
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = i
            }
        }
        return bestIndex
    }

    Box(
        modifier = modifier
            .size(130.dp)
            .semantics {
                role = Role.Tab
                contentDescription = "Exposure duration dial. Currently ${selectedDuration.seconds} seconds"
            }
            .pointerInput(selectedDuration, isProUnlocked) {
                detectTapGestures { offset ->
                    // Coordinates relative to bottom-right corner (size.width, size.height)
                    val dx = offset.x - size.width
                    val dy = offset.y - size.height
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    val idx = findNearestIndex(angle)
                    selectByIndex(idx)
                }
            }
            .pointerInput(selectedDuration, isProUnlocked) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val dx = change.position.x - size.width
                    val dy = change.position.y - size.height
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    val idx = findNearestIndex(angle)
                    selectByIndex(idx)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w, h)
            val arcRadius = w - 16.dp.toPx()

            // Draw Subtle Quarter-Circle Guide Track
            drawArc(
                color = Color(0x33455A64),
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Ticks for each duration option
            durations.forEachIndexed { index, duration ->
                val angleRad = Math.toRadians(angleList[index].toDouble())
                val isSelected = duration == selectedDuration
                val isLocked = duration.isProOnly && !isProUnlocked

                val innerR = arcRadius - (if (isSelected) 10.dp.toPx() else 6.dp.toPx())
                val outerR = arcRadius + (if (isSelected) 4.dp.toPx() else 2.dp.toPx())

                val startX = (center.x + innerR * cos(angleRad)).toFloat()
                val startY = (center.y + innerR * sin(angleRad)).toFloat()
                val endX = (center.x + outerR * cos(angleRad)).toFloat()
                val endY = (center.y + outerR * sin(angleRad)).toFloat()

                val tickColor = when {
                    isSelected -> AccentEmerald
                    isLocked -> Color(0xFF455A64)
                    else -> Color(0xFF78909C)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Render Numerals and Lock Badges at their designated radial positions
        durations.forEachIndexed { index, duration ->
            val angleDeg = angleList[index]
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val isSelected = duration == selectedDuration
            val isLocked = duration.isProOnly && !isProUnlocked

            // Positioning distance from bottom-right corner
            val textDist = 130.dp.value - (if (isSelected) 34f else 30f)
            val xPos = (130f + textDist * cos(angleRad)).toFloat()
            val yPos = (130f + textDist * sin(angleRad)).toFloat()

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (xPos - 14f).dp, y = (yPos - 14f).dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Pro required",
                            tint = Color(0xFF78909C),
                            modifier = Modifier.size(if (isSelected) 12.dp else 10.dp)
                        )
                    }
                    Text(
                        text = "${duration.seconds}",
                        color = when {
                            isSelected -> AccentEmerald
                            isLocked -> Color(0xFF546E7A)
                            else -> TextSecondary
                        },
                        fontSize = if (isSelected) 20.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
