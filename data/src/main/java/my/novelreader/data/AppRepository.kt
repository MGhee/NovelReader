package my.novelreader.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import my.novelreader.core.AppFileResolver
import my.novelreader.core.Response
import my.novelreader.core.isContentUri
import my.novelreader.feature.local_database.AppDatabase
import my.novelreader.feature.local_database.tables.Book
import my.novelreader.feature.local_database.tables.Chapter
import my.novelreader.feature.local_database.tables.ContentType
import my.novelreader.interactor.WorkersInteractions
import my.novelreader.scraper.LightNovelSourceInterface
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.Scraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context,
    val libraryBooks: LibraryBooksRepository,
    val bookChapters: BookChaptersRepository,
    val chapterBody: ChapterBodyRepository,
    val readingStats: ReadingStatsRepository,
    private val appFileResolver: AppFileResolver,
    private val epubImporterRepository: EpubImporterRepository,
    private val scraper: Scraper,
    private val workersInteractions: WorkersInteractions,
) {
    val settings = Settings()
    val eventDataRestored = MutableSharedFlow<Unit>()

    suspend fun toggleBookmark(bookUrl: String, bookTitle: String): Boolean {
        val realUrl = appFileResolver.getLocalIfContentType(bookUrl, bookFolderName = bookTitle)
        return if (bookUrl.isContentUri && libraryBooks.get(realUrl) == null) {
            epubImporterRepository.importEpubFromContentUri(
                contentUri = bookUrl,
                bookTitle = bookTitle,
                addToLibrary = true
            ) is Response.Success
        } else {
            val source = scraper.getCompatibleSource(realUrl)
            // Detect if this is a manga source
            val contentType = if (source is MangaSourceInterface) {
                ContentType.MANGA
            } else {
                ContentType.NOVEL
            }
            val added = libraryBooks.toggleBookmark(
                bookUrl = realUrl,
                bookTitle = bookTitle,
                contentType = contentType,
            )
            // Auto-download volume 1 for fresh light-novel adds (skip re-adds where any
            // volume body is already on disk).
            if (added && source is LightNovelSourceInterface &&
                chapterBody.getDownloadedCount(realUrl) == 0
            ) {
                workersInteractions.downloadFirstVolume(realUrl)
            }
            added
        }
    }

    suspend fun getDatabaseSizeBytes() = withContext(Dispatchers.IO) {
        context.getDatabasePath(db.name).length()
    }

    fun close() = db.closeDatabase()
    @Suppress("unused")
    fun delete() = context.deleteDatabase(db.name)
    suspend fun vacuum() = db.vacuum()

    @Suppress("unused")
    suspend fun <T> withTransaction(fn: suspend () -> T) = db.transaction(fn)

    inner class Settings {
        suspend fun clearNonLibraryData() = withContext(Dispatchers.IO)
        {
            db.libraryDao().removeAllNonLibraryRows()
            db.chapterDao().removeAllNonLibraryRows()
            db.chapterBodyDao().removeAllNonChapterRows()
        }

        /**
         * Folder where additional book data like images is stored.
         * Each subfolder must be an unique folder for each book.
         * Each book folder can have an arbitrary structure internally.
         */
        val folderBooks = appFileResolver.folderBooks
    }

}

fun isValid(book: Book): Boolean = book.url.matches("""^(https?|local)://.*""".toRegex())
fun isValid(chapter: Chapter): Boolean =
    chapter.url.matches("""^(https?|local)://.*""".toRegex())