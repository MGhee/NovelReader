package my.novelreader.data

import kotlinx.coroutines.flow.Flow
import my.novelreader.feature.local_database.AppDatabase
import my.novelreader.feature.local_database.tables.Category
import my.novelreader.feature.local_database.tables.BookCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing reading categories
 */
@Singleton
class CategoriesRepository @Inject constructor(
    private val appDatabase: AppDatabase
) {
    private val categoryDao = appDatabase.categoryDao()
    private val bookCategoryDao = appDatabase.bookCategoryDao()

    // Categories queries
    suspend fun getAllCategories(): List<Category> = categoryDao.getAll()
    fun getAllCategoriesFlow(): Flow<List<Category>> = categoryDao.getAllFlow()

    suspend fun getCategory(id: Int): Category? = categoryDao.get(id)
    fun getCategoryFlow(id: Int): Flow<Category?> = categoryDao.getFlow(id)

    suspend fun createCategory(category: Category) = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(categoryId: Int) = categoryDao.deleteById(categoryId)

    // Book-to-category relationships
    suspend fun getCategoriesForBook(bookUrl: String): List<BookCategory> =
        bookCategoryDao.getCollectionsForBook(bookUrl)

    fun getCategoriesForBookFlow(bookUrl: String): Flow<List<BookCategory>> =
        bookCategoryDao.getCollectionsForBookFlow(bookUrl)

    suspend fun getBooksInCategory(categoryId: Int): List<BookCategory> =
        bookCategoryDao.getBooksInCollection(categoryId)

    fun getBooksInCategoryFlow(categoryId: Int): Flow<List<BookCategory>> =
        bookCategoryDao.getBooksInCollectionFlow(categoryId)

    suspend fun addBookToCategory(bookUrl: String, categoryId: Int) {
        bookCategoryDao.insert(BookCategory(bookUrl = bookUrl, categoryId = categoryId))
    }

    suspend fun removeBookFromCategory(bookUrl: String, categoryId: Int) {
        bookCategoryDao.removeFromCollection(bookUrl, categoryId)
    }

    suspend fun assignBookToCategories(bookUrl: String, categoryIds: Set<Int>) {
        appDatabase.transaction {
            // Remove from all categories first
            bookCategoryDao.removeAllCollectionsForBook(bookUrl)
            // Then add to selected categories
            categoryIds.forEach { categoryId ->
                bookCategoryDao.insert(
                    BookCategory(bookUrl = bookUrl, categoryId = categoryId)
                )
            }
        }
    }

    suspend fun getCategorySize(categoryId: Int): Int =
        bookCategoryDao.getCollectionSize(categoryId)
}
