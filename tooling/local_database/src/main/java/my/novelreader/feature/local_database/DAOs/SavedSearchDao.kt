package my.novelreader.feature.local_database.DAOs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.tables.SavedSearch

@Dao
interface SavedSearchDao {

    @Query("SELECT * FROM SavedSearch ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAll(): List<SavedSearch>

    @Query("SELECT * FROM SavedSearch ORDER BY isPinned DESC, timestamp DESC")
    fun getAllFlow(): Flow<List<SavedSearch>>

    @Query("SELECT * FROM SavedSearch WHERE sourceId = :sourceId ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getBySourceId(sourceId: String): List<SavedSearch>

    @Query("SELECT * FROM SavedSearch WHERE sourceId = :sourceId ORDER BY isPinned DESC, timestamp DESC")
    fun getBySourceIdFlow(sourceId: String): Flow<List<SavedSearch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: SavedSearch)

    @Update
    suspend fun update(search: SavedSearch)

    @Delete
    suspend fun delete(search: SavedSearch)

    @Query("DELETE FROM SavedSearch WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM SavedSearch WHERE sourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: String)

    @Query("UPDATE SavedSearch SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Int, isPinned: Boolean)

    @Query("SELECT COUNT(*) FROM SavedSearch")
    suspend fun getCount(): Int
}
