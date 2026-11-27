package com.company.voltmate.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VoltMateLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    leafColor: Color = DeepGreen,
    boltColor: Color = AccentGreen
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Draw leaf contour
        val leafPath = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            cubicTo(
                w * 0.05f, h * 0.45f,
                w * 0.35f, h * 0.08f,
                w * 0.88f, h * 0.12f
            )
            cubicTo(
                w * 0.95f, h * 0.55f,
                w * 0.65f, h * 0.92f,
                w * 0.15f, h * 0.85f
            )
            close()
        }
        drawPath(
            path = leafPath,
            color = leafColor,
            style = Fill
        )

        // Draw leaf spine
        val spinePath = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            quadraticTo(w * 0.5f, h * 0.55f, w * 0.88f, h * 0.12f)
        }
        drawPath(
            path = spinePath,
            color = Color.White.copy(alpha = 0.35f),
            style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
        )

        // Draw lightning bolt in center
        val boltPath = Path().apply {
            moveTo(w * 0.54f, h * 0.22f)
            lineTo(w * 0.34f, h * 0.52f)
            lineTo(w * 0.48f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.78f)
            lineTo(w * 0.68f, h * 0.44f)
            lineTo(w * 0.52f, h * 0.44f)
            close()
        }
        drawPath(
            path = boltPath,
            color = boltColor,
            style = Fill
        )
    }
}
