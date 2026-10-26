package com.spectreflow.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.ui.theme.AccentEmerald
import com.spectreflow.app.ui.theme.AccentMint
import java.util.Locale

/**
 * Press-and-Hold Shutter Button for Long Exposure.
 *
 * Interaction:
 * - Press & Hold: Starts burst capture, tracks duration, fills progress ring.
 * - Release: Stops capture and initiates processing (if hold >= 1s).
 * - Slide away: Cancels capture if dragged far outside button bounds.
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
    buttonSize: Dp = 88.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 80),
        label = "shutter_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = if (isCapturing)
                    "Exposing long exposure photo. Hold to continue, release to capture"
                else
                    "Shutter button. Press and hold to expose, release to capture"
            }
            .pointerInput(enabled, isCapturing) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val cancelDistance = this.size.width * 1.4f
                        onPressStart()

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

                        if (isCancelled) {
                            onPressCancel()
                        } else {
                            onPressRelease()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val strokeWidth = 5.dp.toPx()
            val diameter = buttonSize.toPx() - strokeWidth
            val radius = diameter / 2f
            val centerOffset = Offset(buttonSize.toPx() / 2f, buttonSize.toPx() / 2f)

            // Outer background track ring
            drawCircle(
                color = Color(0x3300E676),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidth)
            )

            // Active progress ring arc
            if (isCapturing) {
                drawArc(
                    color = AccentEmerald,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                drawCircle(
                    color = AccentEmerald,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Inner Core Content
        if (isCapturing) {
            // Live elapsed time counter formatted as 00:xx
            val secondsInt = elapsedSeconds.toInt()
            val timeText = String.format(Locale.US, "00:%02d", secondsInt)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300E676))
            ) {
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            // Idle Mint shutter disc
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentMint)
            )
        }
    }
}
