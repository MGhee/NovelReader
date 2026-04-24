package my.novelreader.scraper

import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.scraper.domain.BookResult

enum class MangaReadingMode {
    WEBTOON, PAGE_BY_PAGE
}

enum class MangaDirection {
    LTR, RTL
}

interface MangaSourceInterface : SourceInterface.Catalog {
    val readingMode: MangaReadingMode
        get() = MangaReadingMode.WEBTOON

    val direction: MangaDirection
        get() = MangaDirection.LTR

    suspend fun getChapterImages(chapterUrl: String): Response<List<String>>

    /**
     * Get latest manga updates. Return null if not supported by the source.
     */
    override suspend fun latest(page: Int): Response<PagedList<BookResult>>? = null

    /**
     * Get popular manga. Return null if not supported by the source.
     */
    override suspend fun popular(page: Int): Response<PagedList<BookResult>>? = null
}
