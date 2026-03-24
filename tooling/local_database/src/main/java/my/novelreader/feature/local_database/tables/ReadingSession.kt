package my.novelreader.feature.local_database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookUrl: String,
    val startTimeEpochMilli: Long,
    val endTimeEpochMilli: Long = 0,
    val chaptersRead: Int = 0,
)
