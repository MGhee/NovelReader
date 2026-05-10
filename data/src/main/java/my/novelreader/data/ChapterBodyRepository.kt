package my.novelreader.data

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import my.novelreader.core.Response
import my.novelreader.core.isLocalUri
import my.novelreader.core.map
import my.novelreader.feature.local_database.AppDatabase
import my.novelreader.feature.local_database.DAOs.ChapterBodyDao
import my.novelreader.feature.local_database.tables.ChapterBody
import my.novelreader.scraper.LightNovelSourceInterface
import my.novelreader.scraper.Scraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterBodyRepository @Inject constructor(
    private val chapterBodyDao: ChapterBodyDao,
    private val appDatabase: AppDatabase,
    private val bookChaptersRepository: BookChaptersRepository,
    private val downloaderRepository: DownloaderRepository,
    private val libraryBooksRepository: LibraryBooksRepository,
    private val scraper: Scraper,
) {
    companion object {
        private const val MANGA_PAGES_PREFIX = "MANGA_PAGES::"
    }
    suspend fun getAll() = chapterBodyDao.getAll()
    suspend fun insertReplace(chapterBodies: List<ChapterBody>) =
        chapterBodyDao.insertReplace(chapterBodies)

    private suspend fun insertReplace(chapterBody: ChapterBody) =
        chapterBodyDao.insertReplace(chapterBody)

    suspend fun removeRows(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterBodyDao.removeChapterRows(it) }

    private suspend fun insertWithTitle(chapterBody: ChapterBody, title: String?) = appDatabase.transaction {
        insertReplace(chapterBody)
        if (title != null)
            bookChaptersRepository.updateTitle(chapterBody.url, title)
    }

    /**
     * For light-novel sources whose covers come from per-volume EPUB content, refresh the
     * book's cover after a chapter body is fetched. The scraper will have just written the
     * latest volume's cover image to local disk during chapter parse; calling
     * `getBookCoverImageUrl` returns the local cache-busting path which we persist to the
     * book row so the chapters list / library re-render with the new cover.
     */
    private suspend fun refreshLightNovelCoverIfApplicable(chapterUrl: String) {
        try {
            val chapter = bookChaptersRepository.get(chapterUrl) ?: return
            refreshLightNovelCoverForBook(chapter.bookUrl)
        } catch (e: Exception) {
            Log.w("ChapterBodyRepository", "light-novel cover refresh failed for $chapterUrl: ${e.message}")
        }
    }

    private suspend fun refreshLightNovelCoverForBook(bookUrl: String) {
        val source = scraper.getCompatibleSource(bookUrl) as? LightNovelSourceInterface ?: return
        val response = source.getBookCoverImageUrl(bookUrl)
        val newCover = (response as? Response.Success)?.data?.takeIf { it.isNotBlank() } ?: return
        libraryBooksRepository.updateCover(bookUrl, newCover)
    }

    suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String> {
        if (tryCache) chapterBodyDao.get(urlChapter)?.let {
            return@fetchBody Response.Success(it.body)
        }

        if (urlChapter.isLocalUri) {
            return Response.Error(
                """
                Unable to load chapter from url:
                $urlChapter

                Source is local but chapter content missing.
            """.trimIndent(), Exception()
            )
        }

        return downloaderRepository.bookChapter(urlChapter)
            .map {
                insertWithTitle(
                    chapterBody = ChapterBody(url = urlChapter, body = it.body),
                    title = it.title
                )
                refreshLightNovelCoverIfApplicable(urlChapter)
                it.body
            }
    }

    suspend fun downloadChapterBody(chapterUrl: String): Response<Unit> {
        if (chapterUrl.isLocalUri) {
            return Response.Error("Cannot download local chapter content", Exception())
        }

        return downloaderRepository.bookChapter(chapterUrl)
            .map {
                insertWithTitle(
                    chapterBody = ChapterBody(url = chapterUrl, body = it.body),
                    title = it.title
                )
                refreshLightNovelCoverIfApplicable(chapterUrl)
            }
    }

    suspend fun getDownloadedCount(bookUrl: String): Int = chapterBodyDao.getDownloadedChapterCount(bookUrl)

    suspend fun mangaChapterImages(chapterUrl: String): Response<List<String>> =
        downloaderRepository.mangaChapterImages(chapterUrl)

    /**
     * Download chapter content without inserting into DB.
     * Returns the ChapterBody and optional title for batch insertion later.
     */
    suspend fun downloadChapterContent(chapterUrl: String): Response<Pair<ChapterBody, String?>> {
        if (chapterUrl.isLocalUri) {
            return Response.Error("Cannot download local chapter content", Exception())
        }

        return downloaderRepository.bookChapter(chapterUrl)
            .map { download ->
                Pair(ChapterBody(url = chapterUrl, body = download.body), download.title)
            }
    }

    /**
     * Download chapter content directly using the book's source, skipping redirect resolution.
     * Halves HTTP requests for bulk downloads.
     */
    suspend fun downloadChapterContentDirect(chapterUrl: String, bookUrl: String): Response<Pair<ChapterBody, String?>> {
        if (chapterUrl.isLocalUri) {
            return Response.Error("Cannot download local chapter content", Exception())
        }

        return downloaderRepository.bookChapterDirect(chapterUrl, bookUrl)
            .map { download ->
                Pair(ChapterBody(url = chapterUrl, body = download.body), download.title)
            }
    }

    /**
     * Batch insert chapter bodies and update titles in a single transaction.
     */
    suspend fun batchInsertWithTitles(items: List<Pair<ChapterBody, String?>>) {
        if (items.isEmpty()) return
        appDatabase.transaction {
            chapterBodyDao.insertReplace(items.map { it.first })
            for ((body, title) in items) {
                if (title != null) {
                    bookChaptersRepository.updateTitle(body.url, title)
                }
            }
        }
        // For bulk light-novel downloads the scraper rewrote each book's local cover file
        // as it parsed each EPUB; the latest write wins. Refresh once per distinct book
        // instead of per-chapter.
        try {
            val bookUrls = items.mapNotNull { (body, _) ->
                bookChaptersRepository.get(body.url)?.bookUrl
            }.toSet()
            for (bookUrl in bookUrls) {
                refreshLightNovelCoverForBook(bookUrl)
            }
        } catch (e: Exception) {
            Log.w("ChapterBodyRepository", "batch light-novel cover refresh failed: ${e.message}")
        }
    }

    suspend fun saveMangaChapterPages(chapterUrl: String, imageUrls: List<String>) {
        val json = Json.encodeToString(imageUrls)
        insertReplace(ChapterBody(url = chapterUrl, body = "$MANGA_PAGES_PREFIX$json"))
    }

    suspend fun getMangaChapterPages(chapterUrl: String): List<String>? {
        val body = chapterBodyDao.get(chapterUrl) ?: return null
        if (!body.body.startsWith(MANGA_PAGES_PREFIX)) return null
        return try {
            val jsonStr = body.body.substringAfter(MANGA_PAGES_PREFIX)
            val json = Json.parseToJsonElement(jsonStr)
            json.jsonArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            null
        }
    }
}