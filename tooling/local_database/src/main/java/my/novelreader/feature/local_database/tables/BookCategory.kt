package my.novelreader.feature.local_database.tables

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    primaryKeys = ["bookUrl", "categoryId"],
    foreignKeys = [
        ForeignKey(entity = Book::class, parentColumns = ["url"], childColumns = ["bookUrl"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("categoryId"),
        Index("bookUrl"),
        Index(value = ["bookUrl", "categoryId"], unique = true)
    ]
)
data class BookCategory(
    val bookUrl: String,
    val categoryId: Int,
    val addedEpochTimeMilli: Long = System.currentTimeMillis(),
)
