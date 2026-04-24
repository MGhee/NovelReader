package my.novelreader.data

import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.serialization.Serializable
import my.novelreader.feature.local_database.tables.ContentType

/**
 * Represents the current search query state in the library
 */
@Serializable
data class LibrarySearchQuery(
    val text: String = "",
)

/**
 * Sorting options for library display
 */
enum class LibrarySort {
    Title,           // A-Z
    LastRead,        // Most recently read first
    DateAdded,       // Newest additions first
    Completed,       // Completed status
    Unread,          // Books with unread chapters
}

/**
 * Sorting direction
 */
enum class SortDirection {
    Ascending, Descending
}

/**
 * Filtering options for library display
 */
@Serializable
data class LibraryFilters(
    val contentType: ContentType = ContentType.NOVEL,
    val completed: Boolean? = null,  // null = show all, true = only completed, false = only incomplete
    val hasUnread: Boolean? = null,  // null = show all, true = only with unread, false = only read
    val categoryId: Int? = null,      // null = all categories
    val searchQuery: String = "",
) {
    fun isEmpty(): Boolean =
        contentType == ContentType.NOVEL &&
        completed == null &&
        hasUnread == null &&
        categoryId == null &&
        searchQuery.isEmpty()
}

/**
 * Library display modes (from Komikku: CompactGrid, ComfortableGrid, List)
 */
enum class LibraryDisplayMode {
    CompactGrid,      // Small grid items, tight spacing
    ComfortableGrid,  // Medium grid items, comfortable spacing
    List,             // Single-column list with full width items
}

/**
 * Helper to build WHERE clauses for library queries
 */
object LibraryQueryBuilder {
    fun buildWhereClause(filters: LibraryFilters): Pair<String, Array<String>> {
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        whereClauses.add("Book.inLibrary = 1")
        whereClauses.add("Book.contentType = ?")
        args.add(filters.contentType.name)

        if (filters.completed != null) {
            whereClauses.add("Book.completed = ?")
            args.add(if (filters.completed) "1" else "0")
        }

        if (filters.hasUnread != null) {
            if (filters.hasUnread) {
                whereClauses.add("EXISTS (SELECT 1 FROM Chapter WHERE Chapter.bookUrl = Book.url AND Chapter.read = 0)")
            } else {
                whereClauses.add("NOT EXISTS (SELECT 1 FROM Chapter WHERE Chapter.bookUrl = Book.url AND Chapter.read = 0)")
            }
        }

        if (filters.categoryId != null) {
            whereClauses.add("EXISTS (SELECT 1 FROM BookCategory WHERE BookCategory.bookUrl = Book.url AND BookCategory.categoryId = ?)")
            args.add(filters.categoryId.toString())
        }

        if (filters.searchQuery.isNotEmpty()) {
            whereClauses.add("Book.title LIKE ?")
            args.add("%${filters.searchQuery}%")
        }

        return Pair(
            whereClauses.joinToString(" AND "),
            args.toTypedArray()
        )
    }

    fun buildOrderBy(sort: LibrarySort, direction: SortDirection): String {
        val sortColumn = when (sort) {
            LibrarySort.Title -> "Book.title"
            LibrarySort.LastRead -> "Book.lastReadEpochTimeMilli"
            LibrarySort.DateAdded -> "Book.url"  // Use URL as proxy for date added (doesn't change)
            LibrarySort.Completed -> "Book.completed"
            LibrarySort.Unread -> "COUNT(CASE WHEN Chapter.read = 0 THEN 1 END)"
        }
        val directionStr = if (direction == SortDirection.Ascending) "ASC" else "DESC"
        return "$sortColumn $directionStr"
    }

    fun buildLibraryQuery(
        filters: LibraryFilters,
        sort: LibrarySort,
        direction: SortDirection,
        limit: Int = 50,
        offset: Int = 0
    ): SimpleSQLiteQuery {
        val (whereClause, whereArgs) = buildWhereClause(filters)
        val orderBy = buildOrderBy(sort, direction)

        val query = """
            SELECT DISTINCT Book.* FROM Book
            LEFT JOIN Chapter ON Book.url = Chapter.bookUrl
            WHERE $whereClause
            GROUP BY Book.url
            ORDER BY $orderBy
            LIMIT ? OFFSET ?
        """.trimIndent()

        return SimpleSQLiteQuery(
            query,
            whereArgs + arrayOf(limit.toString(), offset.toString())
        )
    }

    fun buildLibraryCountQuery(
        filters: LibraryFilters
    ): SimpleSQLiteQuery {
        val (whereClause, whereArgs) = buildWhereClause(filters)

        val query = """
            SELECT COUNT(DISTINCT Book.url) FROM Book
            LEFT JOIN Chapter ON Book.url = Chapter.bookUrl
            WHERE $whereClause
        """.trimIndent()

        return SimpleSQLiteQuery(query, whereArgs)
    }
}
