package my.novelreader.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.tables.Category

@Dao
interface CategoryDao {

    @Query("SELECT * FROM Category ORDER BY sortOrder ASC")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM Category ORDER BY sortOrder ASC")
    fun getAllFlow(): Flow<List<Category>>

    @Query("SELECT * FROM Category WHERE id = :id")
    suspend fun get(id: Int): Category?

    @Query("SELECT * FROM Category WHERE id = :id")
    fun getFlow(id: Int): Flow<Category?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM Category WHERE id = :id AND id != 0")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM Category")
    suspend fun getCount(): Int
}
