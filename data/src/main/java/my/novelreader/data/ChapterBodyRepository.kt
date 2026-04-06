package my.novelreader.data

import my.novelreader.core.Response
import my.novelreader.core.isLocalUri
import my.novelreader.core.map
import my.novelreader.feature.local_database.AppDatabase
import my.novelreader.feature.local_database.DAOs.ChapterBodyDao
import my.novelreader.feature.local_database.tables.ChapterBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterBodyRepository @Inject constructor(
    private val chapterBodyDao: ChapterBodyDao,
    private val appDatabase: AppDatabase,
    private val bookChaptersRepository: BookChaptersRepository,
    private val downloaderRepository: DownloaderRepository,
) {
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
            }
    }

    suspend fun getDownloadedCount(bookUrl: String): Int = chapterBodyDao.getDownloadedChapterCount(bookUrl)

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
    }
}