package com.spectreflow.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectreflow.app.R
import com.spectreflow.app.sensor.StabilityState
import com.spectreflow.app.ui.theme.AccentEmerald

/**
 * Stability indicator matching reference UI:
 * - A smartphone icon enclosed inside 4 corner brackets [ 📱 ].
 * - Bright green & "STABLE" when steady (tripod-like).
 * - Dim amber/yellow & "SHAKY" when moving.
 */
@Composable
fun StabilityIndicator(
    stability: StabilityState,
    modifier: Modifier = Modifier
) {
    val isStable = stability == StabilityState.STABLE
    val activeColor = if (isStable) AccentEmerald else Color(0xFFFFB300)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Phone glyph inside 4 corner brackets
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 38.dp, height = 38.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 34.dp, height = 34.dp)) {
                val bracketStroke = 2.dp.toPx()
                val bracketLen = 7.dp.toPx()
                val w = size.width
                val h = size.height

                // Top-Left Corner ┌
                drawLine(
                    color = activeColor,
                    start = Offset(0f, 0f),
                    end = Offset(bracketLen, 0f),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, bracketLen),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Top-Right Corner ┐
                drawLine(
                    color = activeColor,
                    start = Offset(w, 0f),
                    end = Offset(w - bracketLen, 0f),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeColor,
                    start = Offset(w, 0f),
                    end = Offset(w, bracketLen),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Bottom-Left Corner └
                drawLine(
                    color = activeColor,
                    start = Offset(0f, h),
                    end = Offset(bracketLen, h),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeColor,
                    start = Offset(0f, h),
                    end = Offset(0f, h - bracketLen),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Bottom-Right Corner ┘
                drawLine(
                    color = activeColor,
                    start = Offset(w, h),
                    end = Offset(w - bracketLen, h),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = activeColor,
                    start = Offset(w, h),
                    end = Offset(w, h - bracketLen),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Phone Body Outline
                val phoneW = 12.dp.toPx()
                val phoneH = 20.dp.toPx()
                val phoneX = (w - phoneW) / 2f
                val phoneY = (h - phoneH) / 2f
                val cornerR = 2.5.dp.toPx()

                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(phoneX, phoneY),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Phone Screen Notch / Speaker Bar
                drawLine(
                    color = activeColor,
                    start = Offset(phoneX + 3.dp.toPx(), phoneY + 3.dp.toPx()),
                    end = Offset(phoneX + phoneW - 3.dp.toPx(), phoneY + 3.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isStable) stringResource(R.string.stability_stable) else stringResource(R.string.stability_shaky),
            color = activeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
    }
}
