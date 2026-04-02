package my.novelreader.personal.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AreaChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    labels: List<String> = emptyList(),
) {
    val animProgress = remember { Animatable(0f) }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        delay(300)
        animProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (data.isEmpty() || data.size < 2) return@Canvas

            val padding = 16.dp.toPx()
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2
            val maxValue = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val progress = animProgress.value

            // Draw grid lines
            val stepCount = 3
            for (i in 0..stepCount) {
                val y = padding + (chartHeight / stepCount) * i
                drawLine(
                    color = surfaceVariantColor.copy(alpha = 0.5f),
                    start = Offset(padding, y),
                    end = Offset(size.width - padding, y),
                    strokeWidth = 0.8f
                )
            }

            // Calculate data points
            val points = mutableListOf<Offset>()
            for (i in data.indices) {
                val x = padding + (chartWidth / (data.size - 1)) * i
                val y = padding + chartHeight - (data[i] / maxValue * chartHeight * progress)
                points.add(Offset(x, y))
            }

            if (points.isNotEmpty()) {
                // Build smooth curve path
                val curvePath = Path()
                curvePath.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx1 = prev.x + (curr.x - prev.x) / 3f
                    val cx2 = prev.x + 2f * (curr.x - prev.x) / 3f
                    curvePath.cubicTo(cx1, prev.y, cx2, curr.y, curr.x, curr.y)
                }

                // Build fill path
                val fillPath = Path()
                fillPath.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx1 = prev.x + (curr.x - prev.x) / 3f
                    val cx2 = prev.x + 2f * (curr.x - prev.x) / 3f
                    fillPath.cubicTo(cx1, prev.y, cx2, curr.y, curr.x, curr.y)
                }
                fillPath.lineTo(points.last().x, padding + chartHeight)
                fillPath.lineTo(points.first().x, padding + chartHeight)
                fillPath.close()

                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.4f * progress),
                            Color.Transparent
                        ),
                        startY = padding,
                        endY = padding + chartHeight
                    )
                )

                // Draw curve line
                drawPath(
                    path = curvePath,
                    color = accentColor.copy(alpha = progress),
                    style = Stroke(width = 2.5f)
                )

                // Draw data points with glow
                for (point in points) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.2f * progress),
                        radius = 8f,
                        center = point
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = progress),
                        radius = 4f,
                        center = point
                    )
                    drawCircle(
                        color = onSurfaceColor.copy(alpha = 0.8f * progress),
                        radius = 2f,
                        center = point
                    )
                }
            }
        }

        // X-axis labels
        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
        }
    }
}
