package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.LanguageCode
import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.postPayload
import my.novelreader.network.postRequest
import my.novelreader.network.toDocument
import my.novelreader.network.tryConnect
import my.novelreader.scraper.R
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.TextExtractor
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


class FreeWebNovel(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "freewebnovel"
    override val nameStrId = R.string.source_name_freewebnovel
    override val baseUrl = "https://freewebnovel.com"
    override val catalogUrl = "https://freewebnovel.com/completed-novel/"
    override val iconUrl = "https://freewebnovel.com/favicon.ico"
    override val language = LanguageCode.ENGLISH

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.selectFirst("h1, .chapter-title")?.text()
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".txt")?.let { element ->
                element.select("script").remove()
                element.select(".ads").remove()
                element.select("h4").remove()
                element.select("sub").remove()
                TextExtractor.get(element)
            } ?: ""
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl)
                    .toDocument()
                    .selectFirst(".pic img")?.attr("src")
                    ?.let {baseUrl + it}
            }


        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl)
                    .toDocument()
                    .selectFirst(".m-desc .txt")
                    ?.let { TextExtractor.get(it) }
            }
        }


    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl)
                .toDocument()
                .select("#idData li a")
                .map {
                    ChapterResult(
                        title = it.selectFirst("a")?.attr("title") ?: "",
                        url = (baseUrl + it.selectFirst("a")?.attr("href"))
                    )
                }
        }
    }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index") {
                val page = index + 1
                val url = "$baseUrl/completed-novel/$page"

                val doc = networkClient.get(url).toDocument()
                val books = doc.select(".ul-list1 .li-row")
                    .mapNotNull { element ->
                        val link = element.selectFirst(".tit a") ?: return@mapNotNull null
                        val coverUrl = element.selectFirst(".pic img")
                            ?.let { it.attr("data-src").ifEmpty { it.attr("src") } } ?: ""
                        BookResult(
                            title = link.text(),
                            url = baseUrl + link.attr("href"),
                            coverImageUrl = baseUrl + coverUrl
                        )
                    }

                PagedList(
                    list = books,
                    index = index,
                    isLastPage =  books.isEmpty() || doc.selectFirst("a:nth-child(13)") == null

                )
            }
        }


    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.IO) {
            tryConnect {
                if (input.isBlank() || index > 0)
                    return@tryConnect PagedList.createEmpty(index = index)
                val url = "https://freewebnovel.com/search"
                val doc: Document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .data("searchkey", input)
                    .timeout(10000)
                    .post()
                val books = doc.select(".ul-list1 .li-row")
                    .mapNotNull { element ->
                        val link = element.selectFirst(".tit a") ?: return@mapNotNull null
                        val coverUrl = element.selectFirst(".pic img")
                            ?.let { it.attr("data-src").ifEmpty { it.attr("src") } } ?: ""
                        BookResult(
                            title = link.text(),
                            url = baseUrl + link.attr("href"),
                            coverImageUrl = baseUrl + coverUrl
                        )
                    }

                PagedList(list = books, index = index, isLastPage = books.isEmpty())
            }
        }

}
