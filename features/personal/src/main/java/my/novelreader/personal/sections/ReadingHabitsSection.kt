package my.novelreader.personal.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.Section
import my.novelreader.coreui.theme.ColorAccent
import my.novelreader.coreui.theme.colorApp
import my.novelreader.personal.charts.ClockChart

@Composable
fun ReadingHabitsSection(
    hourlyData: List<Float>,
    peakHours: String,
    mostActiveDay: String,
    averageSessionMinutes: Int,
) {
    Section(
        title = "Reading Habits",
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorApp.tintedSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtitle explaining the chart
            Text(
                text = "When you read throughout the day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Clock chart (full width, no center label)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                ClockChart(
                    hourlyData = hourlyData,
                    centerLabel = "",
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            // Stats row: 3 equal columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    label = "Peak Hours",
                    value = peakHours
                )
                HabitStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarToday,
                    label = "Most Active",
                    value = mostActiveDay
                )
                HabitStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    label = "Avg. Session",
                    value = formatDuration(averageSessionMinutes)
                )
            }
        }
    }
}

@Composable
private fun HabitStat(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ColorAccent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes == 0 -> "< 1m"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
        }
    }
}
