package my.novelreader.feature.local_database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.feature.local_database.DAOs.BookCategoryDao
import my.novelreader.feature.local_database.DAOs.CategoryDao
import my.novelreader.feature.local_database.DAOs.ChapterBodyDao
import my.novelreader.feature.local_database.DAOs.ChapterDao
import my.novelreader.feature.local_database.DAOs.ChapterTranslationDao
import my.novelreader.feature.local_database.DAOs.LibraryDao
import my.novelreader.feature.local_database.DAOs.ReadingSessionDao
import my.novelreader.feature.local_database.DAOs.SavedSearchDao
import my.novelreader.feature.local_database.tables.Book
import my.novelreader.feature.local_database.tables.BookCategory
import my.novelreader.feature.local_database.tables.Category
import my.novelreader.feature.local_database.tables.Chapter
import my.novelreader.feature.local_database.tables.ChapterBody
import my.novelreader.feature.local_database.tables.ChapterTranslation
import my.novelreader.feature.local_database.tables.LibraryUpdateError
import my.novelreader.feature.local_database.tables.ReadingSession
import my.novelreader.feature.local_database.tables.SavedSearch
import java.io.InputStream


interface AppDatabase {
    fun libraryDao(): LibraryDao
    fun chapterDao(): ChapterDao
    fun chapterBodyDao(): ChapterBodyDao
    fun chapterTranslationDao(): ChapterTranslationDao
    fun readingSessionDao(): ReadingSessionDao
    fun categoryDao(): CategoryDao
    fun bookCategoryDao(): BookCategoryDao
    fun savedSearchDao(): SavedSearchDao
    val name: String

    fun closeDatabase()
    fun clearDatabase()
    suspend fun vacuum()

    /**
     * Execute the whole database calls as an atomic operation
     */
    suspend fun <T> transaction(block: suspend () -> T): T

    companion object {
        fun createRoom(ctx: Context, name: String): AppDatabase = Room
            .databaseBuilder(ctx, AppRoomDatabase::class.java, name)
            .addMigrations(*databaseMigrations())
            .fallbackToDestructiveMigration()
            .build()
            .also { it.name = name }

        fun createRoomFromStream(
            ctx: Context,
            name: String,
            inputStream: InputStream
        ): AppDatabase = Room
            .databaseBuilder(ctx, AppRoomDatabase::class.java, name)
            .createFromInputStream { inputStream }
            .fallbackToDestructiveMigration() // Don't apply migrations, database is already at correct version
            .build()
            .also { it.name = name }
    }
}


@Database(
    entities = [
        Book::class,
        Chapter::class,
        ChapterBody::class,
        ChapterTranslation::class,
        ReadingSession::class,
        Category::class,
        BookCategory::class,
        SavedSearch::class,
        LibraryUpdateError::class
    ],
    version = 19,
    exportSchema = false
)
internal abstract class AppRoomDatabase : RoomDatabase(), AppDatabase {
    abstract override fun libraryDao(): LibraryDao
    abstract override fun chapterDao(): ChapterDao
    abstract override fun chapterBodyDao(): ChapterBodyDao
    abstract override fun chapterTranslationDao(): ChapterTranslationDao
    abstract override fun readingSessionDao(): ReadingSessionDao
    abstract override fun categoryDao(): CategoryDao
    abstract override fun bookCategoryDao(): BookCategoryDao
    abstract override fun savedSearchDao(): SavedSearchDao

    override lateinit var name: String

    override suspend fun <T> transaction(block: suspend () -> T): T = withTransaction(block)

    override fun closeDatabase() {
        close()
    }

    override fun clearDatabase() {
        clearAllTables()
    }

    override suspend fun vacuum() {
        withContext(Dispatchers.IO) { openHelper.writableDatabase.execSQL("VACUUM") }
    }
}
