package my.novelreader.personal.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import my.novelreader.coreui.theme.colorApp
import my.novelreader.coreui.theme.mix
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockChart(
    hourlyData: List<Float>,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    centerLabel: String = "",
) {
    val animProgress = remember { Animatable(0f) }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dynamicAccent = MaterialTheme.colorApp.accent

    // Pre-compute blended segment colors outside Canvas
    val segmentColors = remember(dynamicAccent) {
        listOf(
            // 0-5: Night - deep indigo/blue
            Color(0xFF3949AB).mix(dynamicAccent, 0.3f), Color(0xFF303F9F).mix(dynamicAccent, 0.3f), Color(0xFF283593).mix(dynamicAccent, 0.3f),
            Color(0xFF1A237E).mix(dynamicAccent, 0.3f), Color(0xFF1A237E).mix(dynamicAccent, 0.3f), Color(0xFF283593).mix(dynamicAccent, 0.3f),
            // 6-11: Morning - warm amber/orange
            Color(0xFFFF8F00).mix(dynamicAccent, 0.3f), Color(0xFFFF6F00).mix(dynamicAccent, 0.3f), Color(0xFFFF5722).mix(dynamicAccent, 0.3f),
            Color(0xFFE64A19).mix(dynamicAccent, 0.3f), Color(0xFFD84315).mix(dynamicAccent, 0.3f), Color(0xFFBF360C).mix(dynamicAccent, 0.3f),
            // 12-17: Afternoon - teal/green
            Color(0xFF00897B).mix(dynamicAccent, 0.3f), Color(0xFF00796B).mix(dynamicAccent, 0.3f), Color(0xFF00695C).mix(dynamicAccent, 0.3f),
            Color(0xFF00838F).mix(dynamicAccent, 0.3f), Color(0xFF0097A7).mix(dynamicAccent, 0.3f), Color(0xFF00ACC1).mix(dynamicAccent, 0.3f),
            // 18-23: Evening - purple/violet
            Color(0xFF7B1FA2).mix(dynamicAccent, 0.3f), Color(0xFF8E24AA).mix(dynamicAccent, 0.3f), Color(0xFF9C27B0).mix(dynamicAccent, 0.3f),
            Color(0xFFAB47BC).mix(dynamicAccent, 0.3f), Color(0xFF7E57C2).mix(dynamicAccent, 0.3f), Color(0xFF5C6BC0).mix(dynamicAccent, 0.3f),
        )
    }

    LaunchedEffect(hourlyData) {
        animProgress.snapTo(0f)
        delay(400)
        animProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (hourlyData.isEmpty() || hourlyData.size != 24) return@Canvas

            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = minOf(centerX, centerY) * 0.82f
            val baseRadius = maxRadius * 0.35f
            val progress = animProgress.value

            // Outer guide circle
            drawCircle(
                color = onSurfaceVariantColor.copy(alpha = 0.25f),
                radius = maxRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // Mid guide circle
            drawCircle(
                color = onSurfaceVariantColor.copy(alpha = 0.12f),
                radius = (maxRadius + baseRadius) / 2,
                center = Offset(centerX, centerY),
                style = Stroke(width = 0.5f)
            )

            // Draw ring segments for each hour
            val segmentAngle = 360f / 24f

            for (hour in 0 until 24) {
                val intensity = hourlyData[hour]
                val outerRadius = baseRadius + (intensity * (maxRadius - baseRadius) * progress)
                val startAngle = hour * segmentAngle - 90f
                val sweepAngle = segmentAngle - 1.5f

                val baseColor = segmentColors[hour]

                if (outerRadius > baseRadius + 1f) {
                    val segmentColor = baseColor.copy(
                        alpha = (0.5f + 0.5f * intensity) * progress
                    )

                    val outerRect = Rect(
                        centerX - outerRadius, centerY - outerRadius,
                        centerX + outerRadius, centerY + outerRadius
                    )
                    val innerRect = Rect(
                        centerX - baseRadius, centerY - baseRadius,
                        centerX + baseRadius, centerY + baseRadius
                    )

                    val segmentPath = Path().apply {
                        arcTo(outerRect, startAngle, sweepAngle, false)
                        arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, false)
                        close()
                    }

                    drawPath(segmentPath, segmentColor)

                    // Bright edge on high-intensity segments
                    if (intensity > 0.5f) {
                        val edgePath = Path().apply {
                            arcTo(outerRect, startAngle, sweepAngle, false)
                        }
                        drawPath(
                            edgePath,
                            baseColor.copy(alpha = 0.8f * intensity * progress),
                            style = Stroke(width = 1.5f)
                        )
                    }
                } else {
                    val minRadius = baseRadius + 2f * progress
                    val outerRect = Rect(
                        centerX - minRadius, centerY - minRadius,
                        centerX + minRadius, centerY + minRadius
                    )
                    val innerRect = Rect(
                        centerX - baseRadius, centerY - baseRadius,
                        centerX + baseRadius, centerY + baseRadius
                    )
                    val segmentPath = Path().apply {
                        arcTo(outerRect, startAngle, sweepAngle, false)
                        arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, false)
                        close()
                    }
                    drawPath(segmentPath, surfaceVariantColor.copy(alpha = 0.2f))
                }
            }

            // Hour tick marks at 0, 6, 12, 18
            val tickHours = listOf(0, 6, 12, 18)
            for (hour in tickHours) {
                val angle = Math.toRadians((hour * 15.0 - 90.0))
                val innerX = centerX + ((maxRadius + 4.dp.toPx()) * cos(angle)).toFloat()
                val innerY = centerY + ((maxRadius + 4.dp.toPx()) * sin(angle)).toFloat()
                val outerX = centerX + ((maxRadius + 8.dp.toPx()) * cos(angle)).toFloat()
                val outerY = centerY + ((maxRadius + 8.dp.toPx()) * sin(angle)).toFloat()

                drawLine(
                    color = onSurfaceVariantColor.copy(alpha = 0.6f),
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = 1.5f
                )
            }

            // Center filled circle
            drawCircle(
                color = surfaceVariantColor.copy(alpha = 0.9f),
                radius = baseRadius * 0.7f,
                center = Offset(centerX, centerY)
            )
        }

        // Center label
        if (centerLabel.isNotEmpty()) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
        }

        // Hour labels positioned around the clock
        Text(
            text = "0",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariantColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
        )
        Text(
            text = "6",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariantColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 0.dp)
        )
        Text(
            text = "12",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariantColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )
        Text(
            text = "18",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariantColor,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp)
        )
    }
}
