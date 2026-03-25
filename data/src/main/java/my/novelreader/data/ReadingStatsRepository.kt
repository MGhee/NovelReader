package my.novelreader.data

import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.BookReadingStats
import my.novelreader.feature.local_database.DailyReadingStats
import my.novelreader.feature.local_database.SessionTimestamp
import my.novelreader.feature.local_database.tables.ReadingSession
import my.novelreader.feature.local_database.DAOs.ReadingSessionDao
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar

@Singleton
class ReadingStatsRepository @Inject constructor(
    private val readingSessionDao: ReadingSessionDao,
) {
    suspend fun startSession(bookUrl: String): Long =
        readingSessionDao.insert(ReadingSession(bookUrl = bookUrl, startTimeEpochMilli = System.currentTimeMillis()))

    suspend fun endSession(sessionId: Long, chaptersRead: Int) =
        readingSessionDao.endSession(sessionId, System.currentTimeMillis(), chaptersRead)

    fun totalReadingTime(): Flow<Long?> = readingSessionDao.totalReadingTimeMillis()

    fun perBookStats(): Flow<List<BookReadingStats>> = readingSessionDao.perBookStats()

    fun dailyStats(sinceDaysAgo: Int = 30): Flow<List<DailyReadingStats>> {
        val since = System.currentTimeMillis() - (sinceDaysAgo.toLong() * 86400000)
        return readingSessionDao.dailyStats(since)
    }

    fun totalChaptersReadSince(sinceDaysAgo: Int = 365): Flow<Int?> {
        val since = System.currentTimeMillis() - (sinceDaysAgo.toLong() * 86400000)
        return readingSessionDao.totalChaptersReadSince(since)
    }

    fun chaptersReadToday(): Flow<Int?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return readingSessionDao.totalChaptersReadSince(calendar.timeInMillis)
    }

    fun totalChaptersRead(): Flow<Int?> = readingSessionDao.totalChaptersRead()

    fun averageSessionDuration(): Flow<Long?> = readingSessionDao.averageSessionDurationMillis()

    fun allSessionTimestamps(sinceDaysAgo: Int = 365): Flow<List<SessionTimestamp>> {
        val since = System.currentTimeMillis() - (sinceDaysAgo.toLong() * 86400000)
        return readingSessionDao.allSessionTimestamps(since)
    }

    fun dailyStatsYear(): Flow<List<DailyReadingStats>> = dailyStats(sinceDaysAgo = 365)
}
