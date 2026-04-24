package my.novelreader.data

import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.AppDatabase
import my.novelreader.feature.local_database.tables.SavedSearch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing saved search queries across sources
 */
@Singleton
class SavedSearchRepository @Inject constructor(
    private val appDatabase: AppDatabase
) {
    private val savedSearchDao = appDatabase.savedSearchDao()

    suspend fun getAllSearches(): List<SavedSearch> = savedSearchDao.getAll()
    fun getAllSearchesFlow(): Flow<List<SavedSearch>> = savedSearchDao.getAllFlow()

    suspend fun getSearchesBySource(sourceId: String): List<SavedSearch> =
        savedSearchDao.getBySourceId(sourceId)

    fun getSearchesBySourceFlow(sourceId: String): Flow<List<SavedSearch>> =
        savedSearchDao.getBySourceIdFlow(sourceId)

    suspend fun saveSearch(sourceId: String, query: String) {
        val search = SavedSearch(
            sourceId = sourceId,
            query = query,
            timestamp = System.currentTimeMillis()
        )
        savedSearchDao.insert(search)
    }

    suspend fun updateSearch(search: SavedSearch) {
        savedSearchDao.update(search)
    }

    suspend fun deleteSearch(search: SavedSearch) {
        savedSearchDao.delete(search)
    }

    suspend fun deleteSearchById(id: Int) {
        savedSearchDao.deleteById(id)
    }

    suspend fun deleteSearchesBySource(sourceId: String) {
        savedSearchDao.deleteBySourceId(sourceId)
    }

    suspend fun togglePinned(id: Int, isPinned: Boolean) {
        savedSearchDao.setPinned(id, isPinned)
    }

    suspend fun getSearchCount(): Int = savedSearchDao.getCount()
}
