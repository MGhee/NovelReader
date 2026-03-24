package my.novelreader.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.novelreader.strings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(
    viewModel: PersonalViewModel = viewModel(),
) {
    val totalReadingTime = viewModel.totalReadingTime.collectAsState(null)
    val perBookStats = viewModel.perBookStats.collectAsState(emptyList())
    val dailyStats = viewModel.dailyStats.collectAsState(emptyList())
    val chaptersThisYear = viewModel.chaptersThisYear.collectAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_personal)) }
            )
        },
    ) { paddingValues ->
        if (totalReadingTime.value == null && chaptersThisYear.value == null) {
            // No data yet - show placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Reading Statistics Dashboard",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Open a book and read to track your progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            // Show data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            ) {
                if (totalReadingTime.value != null) {
                    Text(
                        text = "Total reading time: ${formatDuration(totalReadingTime.value ?: 0)}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (chaptersThisYear.value != null) {
                    Text(
                        text = "Chapters read this year: ${chaptersThisYear.value ?: 0}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Text(
                    text = "More detailed stats coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    return when {
        hours > 0 -> "$hours h $minutes m"
        minutes > 0 -> "$minutes m"
        else -> "0 m"
    }
}
