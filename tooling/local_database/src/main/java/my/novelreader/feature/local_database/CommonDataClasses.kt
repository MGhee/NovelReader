package my.novelreader.feature.local_database

import androidx.room.Embedded
import my.novelreader.feature.local_database.tables.Book
import my.novelreader.feature.local_database.tables.Chapter

data class BookMetadata(
    val title: String,
    val url: String,
    val coverImageUrl: String = "",
    val description: String = ""
) {
    override fun equals(other: Any?): Boolean =
        if (other is BookMetadata) (url == other.url) else false

    override fun hashCode(): Int = url.hashCode()
}

data class ChapterMetadata(val title: String, val url: String) {
    override fun equals(other: Any?): Boolean =
        if (other is ChapterMetadata) (url == other.url) else false

    override fun hashCode(): Int = url.hashCode()
}

data class BookWithContext(
    @Embedded val book: Book,
    val chaptersCount: Int,
    val chaptersReadCount: Int,
    val newChaptersCount: Int = 0
)

data class ChapterWithContext(
    @Embedded val chapter: Chapter,
    val downloaded: Boolean,
    val lastReadChapter: Boolean
)

data class BookReadingStats(
    val bookUrl: String,
    val totalTime: Long,
    val totalChapters: Int,
)

data class DailyReadingStats(
    val dayEpoch: Long,
    val totalTimeMillis: Long,
    val totalChapters: Int,
)

data class SessionTimestamp(
    val startTimeEpochMilli: Long,
    val durationMillis: Long,
)