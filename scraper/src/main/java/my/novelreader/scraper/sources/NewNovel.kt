package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.novelreader.core.LanguageCode
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.add
import my.novelreader.network.addPath
import my.novelreader.network.toDocument
import my.novelreader.network.toUrlBuilderSafe
import my.novelreader.network.tryConnect
import my.novelreader.scraper.R
import my.novelreader.scraper.TextExtractor
import my.novelreader.scraper.domain.ChapterResult
import my.novelreader.scraper.templates.BaseNovelFullScraper
import org.jsoup.nodes.Document
import kotlin.text.isBlank
import kotlin.text.substringAfterLast

class NewNovel(
    networkClient: NetworkClient
) : BaseNovelFullScraper(networkClient) {
    override val id = "newnovel"
    override val nameStrId = R.string.source_name_newnovel
    override val baseUrl = "https://newnovel.org/"
    override val catalogUrl = "https://novlove.com/sort/nov-love-daily-update"
    override val iconUrl = "https://newnovel.org/favicon.ico"
    override val language = LanguageCode.ENGLISH

    override val selectCatalogItems = "#list-page .row"
    override val selectCatalogItemTitle = "div.col-xs-7 > div > h3 > a"
    override val selectCatalogItemCover = "div.col-xs-3 > div > img"

    override val selectSearchItems: String = "#list-page .row"
    override val selectSearchItemTitle: String = "div.col-xs-7 > div > h3 > a"
    override val selectSearchItemUrl: String = "a[href]"
    override val selectSearchItemCover: String = "div.col-xs-3 > div > img"
    override val selectPaginationLastPage = "ul.pagination li:last-child"

    fun extractLastPageNumber(doc: Document): Int {
        val lastPageElement = doc.selectFirst("#list-chapter > ul:nth-child(3) > li.last > a")
        val href = lastPageElement?.attr("href")
        if (href == null || href.isBlank()) {
            println("Last page URL not found.")
            return 1
        }
        val pageNumberString = href.substringAfterLast("?page=", "")
        return pageNumberString.toIntOrNull() ?: 1
    }

    override fun isLastPage(doc: org.jsoup.nodes.Document): Boolean {
        val lastLi = doc.selectFirst(selectPaginationLastPage)
        return lastLi == null || lastLi.hasClass("disabled")
    }
    override fun buildSearchUrl(index: Int, input: String): String {
        val page = index + 1
        val builder = baseUrl.toUrlBuilderSafe().addPath("search")
        builder.add("keyword", input)
        if (page > 1) builder.add("page", page)
        return builder.toString()
    }
    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.IO) {
        tryConnect {
            val firstPageUrl = "$bookUrl?page=1"
            val firstDoc = networkClient.get(firstPageUrl).toDocument()
            val totalPages = extractLastPageNumber(firstDoc)
            if (totalPages <= 0) return@tryConnect emptyList()
            val urlsToLoad = (1..totalPages).map { pageIndex ->
                "$bookUrl?page=$pageIndex"
            }
            val deferredDocs = urlsToLoad.map { url ->
                async {
                    networkClient.get(url).toDocument()
                }
            }
            val allDocuments = deferredDocs.awaitAll()
            val allChapters = allDocuments.flatMap { doc ->
                doc.select("ul.list-chapter li a").map { element ->
                    ChapterResult(
                        title = element.text() ?: "",
                        url = ("https://newnovel.org" + element.attr("href"))
                    )
                }
            }
            allChapters
        }
    }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst("div.desc-text")?.text()
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        doc.selectFirst("#chapter > div > div > h2 > a > span")?.text() ?: ""

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst("#chapter-content")?.let { element ->
                element.select("script").remove()
                element.select(".ads").remove()
                element.select("div:contains(newnovel.org)").remove()
                element.select("p:contains(If you find any errors)").remove()
                TextExtractor.get(element)
            } ?: ""
        }
}