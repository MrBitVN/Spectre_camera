package com.spectreflow.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GridOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()
        val gridColor = Color(0x33FFFFFF)

        // Vertical lines at 1/3 and 2/3
        val oneThirdW = width / 3f
        val twoThirdsW = width * 2f / 3f
        drawLine(
            color = gridColor,
            start = Offset(oneThirdW, 0f),
            end = Offset(oneThirdW, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(twoThirdsW, 0f),
            end = Offset(twoThirdsW, height),
            strokeWidth = strokeWidth
        )

        // Horizontal lines at 1/3 and 2/3
        val oneThirdH = height / 3f
        val twoThirdsH = height * 2f / 3f
        drawLine(
            color = gridColor,
            start = Offset(0f, oneThirdH),
            end = Offset(width, oneThirdH),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, twoThirdsH),
            end = Offset(width, twoThirdsH),
            strokeWidth = strokeWidth
        )
    }
}
