package my.novelreader.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.tables.BookCategory

@Dao
interface BookCategoryDao {

    @Query("SELECT * FROM BookCategory")
    suspend fun getAll(): List<BookCategory>

    @Query("SELECT * FROM BookCategory WHERE bookUrl = :bookUrl")
    suspend fun getCollectionsForBook(bookUrl: String): List<BookCategory>

    @Query("SELECT * FROM BookCategory WHERE bookUrl = :bookUrl")
    fun getCollectionsForBookFlow(bookUrl: String): Flow<List<BookCategory>>

    @Query("SELECT * FROM BookCategory WHERE categoryId = :categoryId")
    suspend fun getBooksInCollection(categoryId: Int): List<BookCategory>

    @Query("SELECT * FROM BookCategory WHERE categoryId = :categoryId")
    fun getBooksInCollectionFlow(categoryId: Int): Flow<List<BookCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookCategory: BookCategory)

    @Query("DELETE FROM BookCategory WHERE bookUrl = :bookUrl AND categoryId = :categoryId")
    suspend fun removeFromCollection(bookUrl: String, categoryId: Int)

    @Query("DELETE FROM BookCategory WHERE bookUrl = :bookUrl")
    suspend fun removeAllCollectionsForBook(bookUrl: String)

    @Query("SELECT COUNT(*) FROM BookCategory WHERE categoryId = :categoryId")
    suspend fun getCollectionSize(categoryId: Int): Int
}
