package my.novelreader.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import my.novelreader.data.AppRepository
import my.novelreader.feature.local_database.BookReadingStats
import my.novelreader.feature.local_database.DailyReadingStats
import my.novelreader.feature.local_database.SessionTimestamp
import android.util.Log
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class PersonalViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _insightsState = MutableStateFlow(InsightsState())
    val insightsState: StateFlow<InsightsState> = _insightsState

    private val _selectedYear = MutableStateFlow<Int?>(null) // null = All Time

    // Raw unfiltered data
    private var rawDailyStats: List<DailyReadingStats> = emptyList()
    private var rawSessionTimestamps: List<SessionTimestamp> = emptyList()
    private var rawPerBookStats: List<BookReadingStats> = emptyList()
    private var rawTotalChapters: Int = 0
    private var rawTotalTime: Long = 0
    private var rawChaptersToday: Int = 0
    private var rawAvgDuration: Long = 0

    init {
        viewModelScope.launch {
            try {
                // Use onStart to guarantee each flow emits an initial value,
                // and map nullable to non-nullable with defaults
                val totalChaptersFlow = appRepository.readingStats.totalChaptersRead()
                    .map { it ?: 0 }.onStart { emit(0) }
                val chaptersReadTodayFlow = appRepository.readingStats.chaptersReadToday()
                    .map { it ?: 0 }.onStart { emit(0) }
                val totalReadingTimeFlow = appRepository.readingStats.totalReadingTime()
                    .map { it ?: 0L }.onStart { emit(0L) }
                val dailyStatsFlow = appRepository.readingStats.dailyStatsYear()
                    .onStart { emit(emptyList()) }
                val sessionTimestampsFlow = appRepository.readingStats.allSessionTimestamps(sinceDaysAgo = 365 * 3)
                    .onStart { emit(emptyList()) }
                val avgDurationFlow = appRepository.readingStats.averageSessionDuration()
                    .map { it ?: 0L }.onStart { emit(0L) }
                val perBookStatsFlow = appRepository.readingStats.perBookStats()
                    .onStart { emit(emptyList()) }

                combine(
                    totalChaptersFlow,
                    chaptersReadTodayFlow,
                    totalReadingTimeFlow,
                    dailyStatsFlow,
                    sessionTimestampsFlow,
                    avgDurationFlow,
                    perBookStatsFlow,
                    _selectedYear,
                ) { args: Array<Any?> ->
                    rawTotalChapters = args[0] as Int
                    rawChaptersToday = args[1] as Int
                    rawTotalTime = args[2] as Long
                    rawDailyStats = @Suppress("UNCHECKED_CAST") (args[3] as List<DailyReadingStats>)
                    rawSessionTimestamps = @Suppress("UNCHECKED_CAST") (args[4] as List<SessionTimestamp>)
                    rawAvgDuration = args[5] as Long
                    rawPerBookStats = @Suppress("UNCHECKED_CAST") (args[6] as List<BookReadingStats>)
                    val selectedYear = args[7] as? Int

                    Log.d("PersonalViewModel", "Data received: chapters=$rawTotalChapters, time=${rawTotalTime}ms, dailyStats=${rawDailyStats.size}, sessions=${rawSessionTimestamps.size}, perBook=${rawPerBookStats.size}")

                    val topNovels = computeTopNovels(rawPerBookStats)

                    // Compute available years
                    val cal = Calendar.getInstance()
                    val yearsSet = mutableSetOf<Int>()
                    rawDailyStats.forEach { stat ->
                        cal.timeInMillis = stat.dayEpoch * 86400000L
                        yearsSet.add(cal.get(Calendar.YEAR))
                    }
                    val availableYears = yearsSet.sorted().reversed()

                    recomputeState(selectedYear, topNovels, availableYears)
                }.collect { state ->
                    _insightsState.value = state
                }
            } catch (e: Exception) {
                Log.e("PersonalViewModel", "Error loading insights", e)
                _insightsState.value = InsightsState(isLoading = false)
            }
        }
    }

    fun selectYear(year: Int?) {
        _selectedYear.value = year
    }

    private fun recomputeState(selectedYear: Int?, topNovels: List<TopNovelUiItem>, availableYears: List<Int>): InsightsState {
        // Filter by year if selected
        val (dailyStats, sessionTimestamps) = if (selectedYear != null) {
            val cal = Calendar.getInstance()
            cal.set(selectedYear, Calendar.JANUARY, 1, 0, 0, 0)
            val yearStart = cal.timeInMillis / 86400000L

            cal.set(selectedYear, Calendar.DECEMBER, 31, 23, 59, 59)
            val yearEnd = cal.timeInMillis / 86400000L

            val filteredDaily = rawDailyStats.filter { stat ->
                stat.dayEpoch in yearStart..yearEnd
            }

            val filteredSessions = rawSessionTimestamps.filter { session ->
                cal.timeInMillis = session.startTimeEpochMilli
                cal.get(Calendar.YEAR) == selectedYear
            }

            Pair(filteredDaily, filteredSessions)
        } else {
            Pair(rawDailyStats, rawSessionTimestamps)
        }

        val chaptersForYear = dailyStats.sumOf { it.totalChapters }
        val timeForYear = dailyStats.sumOf { it.totalTimeMillis }

        return InsightsState(
            totalChaptersRead = if (selectedYear != null) chaptersForYear else rawTotalChapters,
            chaptersReadToday = if (selectedYear != null && isToday(selectedYear)) {
                rawChaptersToday
            } else if (selectedYear != null) {
                0
            } else {
                rawChaptersToday
            },
            totalReadingTimeMillis = if (selectedYear != null) timeForYear else rawTotalTime,
            currentStreak = computeCurrentStreak(dailyStats),
            bestStreak = computeBestStreak(dailyStats),
            weeklyActivity = computeWeeklyActivity(dailyStats),
            bestDayOfWeek = computeBestDayOfWeek(dailyStats).second,
            bestDayChapters = computeBestDayOfWeek(dailyStats).first,
            dailyChaptersMap = rawDailyStats.associate { it.dayEpoch to it.totalChapters },
            selectedYear = selectedYear,
            availableYears = availableYears,
            hourlyDistribution = computeHourlyDistribution(sessionTimestamps),
            peakHoursLabel = computePeakHoursLabel(sessionTimestamps),
            mostActiveDayOfWeek = computeMostActiveDayOfWeek(dailyStats),
            averageSessionMinutes = if (sessionTimestamps.isNotEmpty()) {
                (sessionTimestamps.sumOf { it.durationMillis }.toDouble() / sessionTimestamps.size / 60000).toInt()
            } else {
                (rawAvgDuration / 60000).toInt()
            },
            topNovels = topNovels,
            isLoading = false,
        )
    }

    private fun isToday(year: Int): Boolean {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == year
    }

    private fun computeCurrentStreak(dailyStats: List<DailyReadingStats>): Int {
        if (dailyStats.isEmpty()) return 0

        val today = System.currentTimeMillis() / 86400000
        val sortedByDay = dailyStats.sortedByDescending { it.dayEpoch }

        var streak = 0
        var currentDay = today

        for (stat in sortedByDay) {
            if (stat.dayEpoch == currentDay && stat.totalChapters > 0) {
                streak++
                currentDay--
            } else if (stat.dayEpoch < currentDay) {
                break
            }
        }

        return streak
    }

    private fun computeBestStreak(dailyStats: List<DailyReadingStats>): Int {
        if (dailyStats.isEmpty()) return 0

        val sorted = dailyStats.sortedBy { it.dayEpoch }
        var maxStreak = 0
        var currentStreak = 0

        for (i in sorted.indices) {
            if (sorted[i].totalChapters > 0) {
                if (i == 0 || sorted[i].dayEpoch - sorted[i - 1].dayEpoch == 1L) {
                    currentStreak++
                    maxStreak = max(maxStreak, currentStreak)
                } else {
                    currentStreak = 1
                    maxStreak = max(maxStreak, currentStreak)
                }
            } else {
                currentStreak = 0
            }
        }

        return maxStreak
    }

    private fun computeWeeklyActivity(dailyStats: List<DailyReadingStats>): List<Float> {
        val dayOfWeekCounts = IntArray(7)
        val dayOfWeekDays = IntArray(7)

        for (stat in dailyStats) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = stat.dayEpoch * 86400000L
            val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1) % 7
            dayOfWeekDays[dayOfWeek]++
            dayOfWeekCounts[dayOfWeek] += stat.totalChapters
        }

        return dayOfWeekCounts.indices.map { i ->
            if (dayOfWeekDays[i] > 0) dayOfWeekCounts[i].toFloat() / dayOfWeekDays[i] else 0f
        }
    }

    private fun computeBestDayOfWeek(dailyStats: List<DailyReadingStats>): Pair<Int, String> {
        val dayOfWeekCounts = IntArray(7)
        val dayOfWeekDays = IntArray(7)
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (stat in dailyStats) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = stat.dayEpoch * 86400000L
            val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1) % 7
            dayOfWeekDays[dayOfWeek]++
            dayOfWeekCounts[dayOfWeek] += stat.totalChapters
        }

        val bestIndex = dayOfWeekCounts.indices.maxByOrNull { if (dayOfWeekDays[it] > 0) dayOfWeekCounts[it] / dayOfWeekDays[it] else 0 } ?: 0
        return Pair(dayOfWeekCounts[bestIndex], dayNames[bestIndex])
    }

    private fun computeMostActiveDayOfWeek(dailyStats: List<DailyReadingStats>): String {
        return computeBestDayOfWeek(dailyStats).second
    }

    private fun computeHourlyDistribution(sessionTimestamps: List<SessionTimestamp>): List<Float> {
        if (sessionTimestamps.isEmpty()) return List(24) { 0f }

        val hourCounts = IntArray(24)
        val cal = Calendar.getInstance()

        for (session in sessionTimestamps) {
            cal.timeInMillis = session.startTimeEpochMilli
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour]++
        }

        val maxCount = hourCounts.maxOrNull()?.toFloat() ?: 1f
        return hourCounts.map { it.toFloat() / maxCount }
    }

    private fun computePeakHoursLabel(sessionTimestamps: List<SessionTimestamp>): String {
        if (sessionTimestamps.isEmpty()) return ""

        val hourCounts = IntArray(24)
        val cal = Calendar.getInstance()

        for (session in sessionTimestamps) {
            cal.timeInMillis = session.startTimeEpochMilli
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour]++
        }

        val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 0
        val nextHour = (peakHour + 1) % 24

        return String.format("%02d:00-%02d:00", peakHour, nextHour)
    }

    private suspend fun computeTopNovels(
        perBookStats: List<BookReadingStats>,
    ): List<TopNovelUiItem> {
        val topByTime = perBookStats
            .sortedByDescending { it.totalTime }
            .take(5)
            .mapNotNull { stat ->
                val book = appRepository.libraryBooks.get(stat.bookUrl)
                val bookTitle = book?.title ?: stat.bookUrl.substringAfterLast("/").replace("-", " ")
                    .replaceFirstChar { it.uppercase() }
                val coverUrl = book?.coverImageUrl ?: ""
                val totalChapters = book?.lastSeenChaptersCount?.takeIf { it > 0 } ?: stat.totalChapters
                TopNovelUiItem(
                    title = bookTitle,
                    coverImageUrl = coverUrl,
                    chaptersRead = stat.totalChapters,
                    totalChapters = totalChapters,
                    readingTimeMillis = stat.totalTime,
                )
            }

        return topByTime
    }
}
