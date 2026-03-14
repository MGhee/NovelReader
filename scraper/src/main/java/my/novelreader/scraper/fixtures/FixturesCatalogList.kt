package my.novelreader.scraper.fixtures

import my.novelreader.core.LanguageCode
import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.scraper.R
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult

fun fixturesCatalogList(): List<SourceInterface.Catalog> = (0..7).map {
    object : SourceInterface.Catalog {
        override val catalogUrl = "catalogUrl$it"
        override val language = LanguageCode.ENGLISH
        override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
            Response.Success(listOf())

        override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun getCatalogSearch(
            index: Int,
            input: String
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override val id = "id$it"
        override val nameStrId = R.string.source_name_local
        override val baseUrl = "baseUrl$it"
    }
}