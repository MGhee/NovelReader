package my.novelreader.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.tables.ReadingSession
import my.novelreader.feature.local_database.BookReadingStats
import my.novelreader.feature.local_database.DailyReadingStats

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSession): Long

    @Query("UPDATE ReadingSession SET endTimeEpochMilli = :endTime, chaptersRead = :chaptersRead WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long, chaptersRead: Int)

    @Query("SELECT * FROM ReadingSession WHERE endTimeEpochMilli > 0 ORDER BY startTimeEpochMilli DESC")
    fun getAllCompleted(): Flow<List<ReadingSession>>

    @Query("SELECT SUM(endTimeEpochMilli - startTimeEpochMilli) FROM ReadingSession WHERE endTimeEpochMilli > 0")
    fun totalReadingTimeMillis(): Flow<Long?>

    @Query("SELECT SUM(endTimeEpochMilli - startTimeEpochMilli) FROM ReadingSession WHERE endTimeEpochMilli > 0 AND bookUrl = :bookUrl")
    fun totalReadingTimeForBook(bookUrl: String): Flow<Long?>

    @Query("""
        SELECT bookUrl, SUM(endTimeEpochMilli - startTimeEpochMilli) as totalTime, SUM(chaptersRead) as totalChapters
        FROM ReadingSession WHERE endTimeEpochMilli > 0
        GROUP BY bookUrl ORDER BY totalTime DESC
    """)
    fun perBookStats(): Flow<List<BookReadingStats>>

    @Query("""
        SELECT SUM(chaptersRead) FROM ReadingSession
        WHERE endTimeEpochMilli > 0 AND startTimeEpochMilli > :sinceEpochMilli
    """)
    fun totalChaptersReadSince(sinceEpochMilli: Long): Flow<Int?>

    @Query("""
        SELECT (startTimeEpochMilli / 86400000) as dayEpoch,
               SUM(endTimeEpochMilli - startTimeEpochMilli) as totalTimeMillis,
               SUM(chaptersRead) as totalChapters
        FROM ReadingSession
        WHERE endTimeEpochMilli > 0 AND startTimeEpochMilli > :sinceEpochMilli
        GROUP BY dayEpoch ORDER BY dayEpoch ASC
    """)
    fun dailyStats(sinceEpochMilli: Long): Flow<List<DailyReadingStats>>
}
