package my.novelreader.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.novelreader.coreui.theme.ColorAccent
import my.novelreader.personal.sections.HeaderStatsCards
import my.novelreader.personal.sections.ReadingActivitySection
import my.novelreader.personal.sections.ReadingCalendarSection
import my.novelreader.personal.sections.ReadingHabitsSection
import my.novelreader.personal.sections.TopNovelsSection
import my.novelreader.strings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(
    viewModel: PersonalViewModel = viewModel(),
) {
    val state = viewModel.insightsState.collectAsState()
    val showYearMenu = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val yearDisplay = if (state.value.selectedYear != null) {
                        "Personal · ${state.value.selectedYear}"
                    } else {
                        "Personal · All Time"
                    }
                    Text(yearDisplay)
                },
                actions = {
                    IconButton(onClick = { showYearMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Select year")
                    }
                    DropdownMenu(
                        expanded = showYearMenu.value,
                        onDismissRequest = { showYearMenu.value = false }
                    ) {
                        // "All Time" option
                        DropdownMenuItem(
                            text = { Text("All Time") },
                            onClick = {
                                viewModel.selectYear(null)
                                showYearMenu.value = false
                            }
                        )
                        // Individual years
                        state.value.availableYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    viewModel.selectYear(year)
                                    showYearMenu.value = false
                                }
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        if (state.value.isLoading) {
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
                        text = "Loading your reading data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Header stats cards
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    HeaderStatsCards(
                        totalChapters = state.value.totalChaptersRead,
                        chaptersToday = state.value.chaptersReadToday,
                        totalTimeMillis = state.value.totalReadingTimeMillis,
                        currentStreak = state.value.currentStreak,
                        bestStreak = state.value.bestStreak,
                        visible = !state.value.isLoading
                    )
                }

                // Reading activity chart
                if (state.value.weeklyActivity.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ReadingActivitySection(
                            weeklyData = state.value.weeklyActivity,
                            bestDayOfWeek = state.value.bestDayOfWeek,
                            bestDayChapters = state.value.bestDayChapters
                        )
                    }
                }

                // Reading calendar
                if (state.value.dailyChaptersMap.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ReadingCalendarSection(
                            dailyChapters = state.value.dailyChaptersMap,
                            selectedYear = state.value.selectedYear
                        )
                    }
                }

                // Reading Habits (full width)
                if (state.value.hourlyDistribution.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ReadingHabitsSection(
                            hourlyData = state.value.hourlyDistribution,
                            peakHours = state.value.peakHoursLabel,
                            mostActiveDay = state.value.mostActiveDayOfWeek,
                            averageSessionMinutes = state.value.averageSessionMinutes
                        )
                    }
                }

                // Top Novels (full width)
                if (state.value.topNovels.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TopNovelsSection(topNovels = state.value.topNovels)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
