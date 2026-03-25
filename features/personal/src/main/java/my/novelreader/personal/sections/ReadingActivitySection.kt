package my.novelreader.personal.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.Section
import my.novelreader.coreui.theme.ColorAccent
import my.novelreader.coreui.theme.colorApp
import my.novelreader.personal.charts.AreaChart

@Composable
fun ReadingActivitySection(
    weeklyData: List<Float>,
    bestDayOfWeek: String,
    bestDayChapters: Int,
) {
    Section(
        title = "Reading Activity",
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorApp.tintedSurface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // "Best Day" + "Chapters" labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Best Day: $bestDayOfWeek ($bestDayChapters chapters)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Chapters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )

            AreaChart(
                data = weeklyData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                accentColor = ColorAccent,
                labels = listOf("S", "M", "T", "W", "T", "F", "S")
            )
        }
    }
}
