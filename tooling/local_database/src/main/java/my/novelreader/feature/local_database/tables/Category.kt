package my.novelreader.feature.local_database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import my.novelreader.feature.local_database.tables.ContentType

@Serializable
@Entity
data class Category(
    @PrimaryKey val id: Int,
    val name: String,
    val sortOrder: Int,
    val flags: Int = 0,
    val contentType: ContentType = ContentType.NOVEL,
)
