package my.novelreader.personal.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import my.novelreader.coreui.theme.colorApp
import java.util.Calendar

@Composable
fun CalendarHeatmap(
    dailyChapters: Map<Long, Int>,
    year: Int,
    modifier: Modifier = Modifier,
) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animProgress = remember(year) { mutableFloatStateOf(0f) }
    LaunchedEffect(year, dailyChapters) {
        animProgress.floatValue = 0f
        delay(200)
        for (i in 0..60) {
            animProgress.floatValue = i / 60f
            delay(10)
        }
    }

    // Compute start and end day epochs for the selected year
    val cal = Calendar.getInstance()
    val selectedYear = year ?: cal.get(Calendar.YEAR)

    cal.set(selectedYear, Calendar.JANUARY, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val yearStartEpoch = cal.timeInMillis / 86400000L

    val currentDayEpoch = System.currentTimeMillis() / 86400000L

    // End of year or today, whichever is earlier
    cal.set(selectedYear, Calendar.DECEMBER, 31, 0, 0, 0)
    val yearEndEpoch = minOf(cal.timeInMillis / 86400000L, currentDayEpoch)

    // Find the day-of-week of Jan 1 (0=Sun, 6=Sat)
    cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
    val jan1DayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1) // 0=Sun

    // Number of weeks needed
    val totalDays = (yearEndEpoch - yearStartEpoch + 1).toInt() + jan1DayOfWeek
    val weeksToShow = (totalDays + 6) / 7

    // Filter data for this year only
    val yearData = dailyChapters.filter { (dayEpoch, _) ->
        dayEpoch in yearStartEpoch..yearEndEpoch
    }

    val cellSizeDp = 11.dp
    val cellGapDp = 2.dp
    val leftLabelWidthDp = 20.dp
    val topLabelHeightDp = 16.dp

    val totalWidthDp = leftLabelWidthDp + (cellSizeDp + cellGapDp) * weeksToShow + 4.dp
    val totalHeightDp = topLabelHeightDp + (cellSizeDp + cellGapDp) * 7 + 4.dp

    val scrollState = rememberScrollState(Int.MAX_VALUE)

    LaunchedEffect(year) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val color1 = MaterialTheme.colorApp.calendarHeat1
    val color2 = MaterialTheme.colorApp.calendarHeat2
    val color3 = MaterialTheme.colorApp.calendarHeat3
    val color4 = MaterialTheme.colorApp.calendarHeat4

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidthDp)
                    .height(totalHeightDp)
            ) {
                val cellSize = cellSizeDp.toPx()
                val cellGap = cellGapDp.toPx()
                val leftLabel = leftLabelWidthDp.toPx()
                val topLabel = topLabelHeightDp.toPx()
                val progress = animProgress.floatValue

                val maxChapters = (yearData.values.maxOrNull() ?: 1).coerceAtLeast(1)

                val colorEmpty = emptyColor

                fun getColor(chapters: Int): androidx.compose.ui.graphics.Color {
                    return when {
                        chapters == 0 -> colorEmpty
                        chapters <= maxChapters / 4 -> color1
                        chapters <= maxChapters / 2 -> color2
                        chapters <= (maxChapters * 3) / 4 -> color3
                        else -> color4
                    }
                }

                // Month labels
                val textPaint = Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 9.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val monthNames = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val monthCal = Calendar.getInstance()
                var lastMonth = -1
                for (weekIndex in 0 until weeksToShow) {
                    // First day in this week column
                    val daysFromStart = weekIndex * 7 - jan1DayOfWeek
                    if (daysFromStart < 0) continue
                    val dayEpoch = yearStartEpoch + daysFromStart
                    monthCal.timeInMillis = dayEpoch * 86400000L
                    val month = monthCal.get(Calendar.MONTH)
                    if (month != lastMonth) {
                        val x = leftLabel + weekIndex * (cellSize + cellGap) + cellSize / 2
                        drawContext.canvas.nativeCanvas.drawText(
                            monthNames[month], x, topLabel - 3.dp.toPx(), textPaint
                        )
                        lastMonth = month
                    }
                }

                // Day labels
                val dayLabels = listOf("", "M", "", "W", "", "F", "")
                val dayLabelPaint = Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 8.dp.toPx()
                    textAlign = Paint.Align.LEFT
                    isAntiAlias = true
                }
                for (dayOfWeek in 0 until 7) {
                    if (dayLabels[dayOfWeek].isNotEmpty()) {
                        val y = topLabel + dayOfWeek * (cellSize + cellGap) + cellSize * 0.85f
                        drawContext.canvas.nativeCanvas.drawText(
                            dayLabels[dayOfWeek], 2.dp.toPx(), y, dayLabelPaint
                        )
                    }
                }

                // Draw cells
                val visibleWeeks = (weeksToShow * progress).toInt().coerceAtMost(weeksToShow)
                for (weekIndex in 0 until weeksToShow) {
                    for (dayOfWeek in 0 until 7) {
                        // Skip cells before Jan 1
                        val daysFromStart = weekIndex * 7 + dayOfWeek - jan1DayOfWeek
                        if (daysFromStart < 0) continue

                        val dayEpoch = yearStartEpoch + daysFromStart
                        if (dayEpoch > yearEndEpoch) continue

                        val chapters = yearData[dayEpoch] ?: 0
                        val x = leftLabel + weekIndex * (cellSize + cellGap)
                        val y = topLabel + dayOfWeek * (cellSize + cellGap)

                        val cellColor = if (weekIndex < visibleWeeks) {
                            getColor(chapters)
                        } else {
                            colorEmpty
                        }

                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(x, y),
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(2.5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
