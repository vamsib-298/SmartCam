package com.example.smartcam.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.smartcam.pose.PoseLandmarks
import com.example.smartcam.pose.PosePoint

/**
 * Rule-of-thirds grid to help framing. Drawn under the pose overlay.
 */
@Composable
fun CompositionGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val line = Color(0x55FFFFFF)
        val w = size.width
        val h = size.height
        for (i in 1..2) {
            val x = w * i / 3f
            drawLine(line, Offset(x, 0f), Offset(x, h), strokeWidth = 2f)
            val y = h * i / 3f
            drawLine(line, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
        }
    }
}

/**
 * Draws the translucent target pose silhouette and the user's live skeleton on
 * top of the camera preview. Both use normalized coordinates, so the overlay
 * lines up with the full-screen preview.
 */
@Composable
fun PoseOverlay(
    template: Map<Int, PosePoint>,
    live: Map<Int, PosePoint>?,
    matched: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Target outline: thick translucent white glow, like a model to step into.
        drawSkeleton(
            points = template,
            color = if (matched) Color(0xCC4CFF7A) else Color(0xB3FFFFFF),
            strokeWidth = 18f,
            headRadius = 42f
        )
        // Live skeleton: thinner accent line tracking the user.
        live?.let {
            drawSkeleton(
                points = it,
                color = if (matched) Color(0xFF4CFF7A) else Color(0xFF38BDF8),
                strokeWidth = 8f,
                headRadius = 26f
            )
        }
    }
}

private fun DrawScope.drawSkeleton(
    points: Map<Int, PosePoint>,
    color: Color,
    strokeWidth: Float,
    headRadius: Float
) {
    fun toOffset(p: PosePoint) = Offset(p.x * size.width, p.y * size.height)

    for ((from, to) in PoseLandmarks.BONES) {
        val a = points[from] ?: continue
        val b = points[to] ?: continue
        drawLine(
            color = color,
            start = toOffset(a),
            end = toOffset(b),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
    points[PoseLandmarks.NOSE]?.let { nose ->
        drawCircle(
            color = color,
            radius = headRadius,
            center = toOffset(nose),
            style = Stroke(width = strokeWidth)
        )
    }
}
