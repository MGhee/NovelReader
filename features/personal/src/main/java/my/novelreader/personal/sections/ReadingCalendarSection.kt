package my.novelreader.personal.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.novelreader.coreui.components.Section
import my.novelreader.coreui.theme.colorApp
import my.novelreader.personal.charts.CalendarHeatmap

@Composable
fun ReadingCalendarSection(
    dailyChapters: Map<Long, Int>,
    selectedYear: Int?,
) {
    Section(
        title = "Reading Calendar",
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorApp.tintedSurface
        )
    ) {
        val displayYear = selectedYear ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        CalendarHeatmap(
            dailyChapters = dailyChapters,
            year = displayYear,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}
