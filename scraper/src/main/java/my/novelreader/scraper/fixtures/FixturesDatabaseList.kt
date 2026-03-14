package my.novelreader.scraper.fixtures

import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.scraper.DatabaseInterface
import my.novelreader.scraper.R
import my.novelreader.scraper.domain.BookResult

fun fixturesDatabaseList(): List<DatabaseInterface> = (0..2).map {
    object : DatabaseInterface {
        override val id = "id$it"
        override val nameStrId = R.string.database_name_baka_updates
        override val baseUrl = "baseUrl$it"
        override suspend fun getCatalog(index: Int): Response<PagedList<BookResult>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun searchByTitle(
            index: Int,
            input: String
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override suspend fun searchByFilters(
            index: Int,
            genresIncludedId: List<String>,
            genresExcludedId: List<String>
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override suspend fun getSearchFilters(): Response<List<my.novelreader.scraper.SearchGenre>> =
            Response.Success(listOf())

        override suspend fun getBookData(bookUrl: String): Response<DatabaseInterface.BookData> =
            throw NotImplementedError()

        override suspend fun getAuthorData(authorUrl: String): Response<DatabaseInterface.AuthorData> =
            throw NotImplementedError()
    }
}

