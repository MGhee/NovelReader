package my.novelreader.feature.local_database.tables

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(indices = [Index("bookUrl")])
data class Chapter(
    val title: String,
    @PrimaryKey val url: String,
    val bookUrl: String,
    val position: Int,
    val read: Boolean = false,
    val lastReadPosition: Int = 0,
    val lastReadOffset: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val bookmarked: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val lastReadMangaPage: Int = 0,
)