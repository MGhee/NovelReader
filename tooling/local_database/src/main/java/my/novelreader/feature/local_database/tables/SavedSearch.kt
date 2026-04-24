package my.novelreader.feature.local_database.tables

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a saved search query for quick access
 */
@Serializable
@Entity(indices = [Index("sourceId")])
data class SavedSearch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceId: String,  // Source ID from SourceInterface
    val query: String,     // Search query text
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
)
