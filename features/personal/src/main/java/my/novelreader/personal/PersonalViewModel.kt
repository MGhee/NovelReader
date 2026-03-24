package my.novelreader.personal

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import my.novelreader.data.AppRepository
import my.novelreader.feature.local_database.BookReadingStats
import my.novelreader.feature.local_database.DailyReadingStats
import javax.inject.Inject

@HiltViewModel
class PersonalViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    val totalReadingTime: Flow<Long?> = appRepository.readingStats.totalReadingTime()

    val perBookStats: Flow<List<BookReadingStats>> = appRepository.readingStats.perBookStats()

    val dailyStats: Flow<List<DailyReadingStats>> = appRepository.readingStats.dailyStats(sinceDaysAgo = 30)

    val chaptersThisYear: Flow<Int?> = appRepository.readingStats.totalChaptersReadSince(sinceDaysAgo = 365)
}
