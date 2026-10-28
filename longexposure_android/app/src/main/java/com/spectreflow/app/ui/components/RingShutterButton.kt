package com.spectreflow.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.ui.theme.AccentEmerald
import java.util.Locale

/**
 * Large dual-ring shutter button matching reference design:
 * - Two concentric green rings: outer ring + inner ring.
 * - Press and hold to expose, release to capture.
 * - While holding: inner ring pulses, outer ring arc fills toward cap, elapsed time "00:07" displayed.
 * - Tactile haptic pulse on press down and release.
 */
@Composable
fun RingShutterButton(
    isCapturing: Boolean,
    progress: Float,
    elapsedSeconds: Float,
    onPressStart: () -> Unit,
    onPressRelease: () -> Unit,
    onPressCancel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: Dp = 82.dp
) {
    val view = LocalView.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 80),
        label = "shutter_progress"
    )

    // Pulse animation for inner ring during capture
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shutter_pulse"
    )

    val shutterCapturingDesc = stringResource(R.string.cd_shutter_capturing)
    val shutterIdleDesc = stringResource(R.string.cd_shutter_idle)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = if (isCapturing) shutterCapturingDesc else shutterIdleDesc
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val cancelDistance = size.width * 1.5f
                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onPressStart()
                            } catch (t: Throwable) {
                                android.util.Log.e("RingShutterButton", "onPressStart failed", t)
                            }

                            var isCancelled = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val current = event.changes.firstOrNull { it.id == down.id }
                                if (current == null || !current.pressed) {
                                    // Pointer lifted up
                                    break
                                }
                                val dist = (current.position - center).getDistance()
                                if (dist > cancelDistance) {
                                    isCancelled = true
                                    break
                                }
                            }

                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (isCancelled) {
                                    onPressCancel()
                                } else {
                                    onPressRelease()
                                }
                            } catch (t: Throwable) {
                                android.util.Log.e("RingShutterButton", "onPressRelease/Cancel failed", t)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("RingShutterButton", "Pointer event loop aborted safely", t)
                }
            }
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val strokeW = 3.5.dp.toPx()
            val outerDiameter = buttonSize.toPx() - strokeW
            val outerRadius = outerDiameter / 2f
            val centerOffset = Offset(buttonSize.toPx() / 2f, buttonSize.toPx() / 2f)

            // 1. Outer Ring
            if (isCapturing) {
                // Dim background track
                drawCircle(
                    color = Color(0x331ED98B),
                    radius = outerRadius,
                    center = centerOffset,
                    style = Stroke(width = strokeW)
                )
                // Active progress arc
                drawArc(
                    color = AccentEmerald,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(strokeW / 2f, strokeW / 2f),
                    size = Size(outerDiameter, outerDiameter),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Idle outer green circle stroke
                drawCircle(
                    color = AccentEmerald,
                    radius = outerRadius,
                    center = centerOffset,
                    style = Stroke(width = strokeW)
                )
            }

            // 2. Inner Ring
            val baseInnerRadius = outerRadius * 0.72f
            val effectiveInnerRadius = if (isCapturing) baseInnerRadius * pulseScale else baseInnerRadius
            val innerStrokeW = if (isCapturing) 3.dp.toPx() else 3.5.dp.toPx()

            drawCircle(
                color = AccentEmerald,
                radius = effectiveInnerRadius,
                center = centerOffset,
                style = Stroke(width = innerStrokeW)
            )
        }

        // 3. Center Elapsed Time Readout during capture
        if (isCapturing) {
            val secondsInt = elapsedSeconds.toInt()
            val timeText = String.format(Locale.US, "00:%02d", secondsInt)

            Text(
                text = timeText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}
