package my.novelreader.feature.local_database.tables

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks errors during library update attempts
 */
@Entity(indices = [Index("bookUrl")])
data class LibraryUpdateError(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookUrl: String,
    val errorMessage: String,
    val errorType: String,  // e.g., "network", "parse", "timeout"
    val attemptCount: Int = 1,
    val lastAttemptTime: Long = System.currentTimeMillis(),
)
