package my.novelreader.personal

data class InsightsState(
    // Header stat cards
    val totalChaptersRead: Int = 0,
    val chaptersReadToday: Int = 0,
    val totalReadingTimeMillis: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,

    // Reading activity (last 28 days, aggregated by day-of-week: Sun-Sat)
    val weeklyActivity: List<Float> = emptyList(),
    val bestDayOfWeek: String = "",
    val bestDayChapters: Int = 0,

    // Calendar heatmap (all data: dayEpoch -> chapters)
    val dailyChaptersMap: Map<Long, Int> = emptyMap(),
    val selectedYear: Int? = null, // null = All Time
    val availableYears: List<Int> = emptyList(),

    // Reading habits
    val hourlyDistribution: List<Float> = emptyList(), // 24 values, normalized 0-1
    val peakHoursLabel: String = "",
    val mostActiveDayOfWeek: String = "",
    val averageSessionMinutes: Int = 0,

    // Top novels
    val topNovels: List<TopNovelUiItem> = emptyList(),

    val isLoading: Boolean = true,
)

data class TopNovelUiItem(
    val title: String,
    val coverImageUrl: String,
    val chaptersRead: Int,
    val totalChapters: Int,
    val readingTimeMillis: Long,
)
