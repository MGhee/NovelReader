package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.LanguageCode
import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.add
import my.novelreader.network.addPath
import my.novelreader.network.ifCase
import my.novelreader.network.postRequest
import my.novelreader.network.toDocument
import my.novelreader.network.toUrlBuilderSafe
import my.novelreader.network.tryConnect
import my.novelreader.scraper.R
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.TextExtractor
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

class Novelku(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "novelku"
    override val nameStrId = R.string.source_name_novelku
    override val baseUrl = "https://novelku.id/"
    override val catalogUrl = "https://novelku.id/"
    override val iconUrl =
        "https://novelku.id/wp-content/uploads/2022/03/cropped-fvc-novelku.id_-32x32.png"
    override val language = LanguageCode.INDONESIAN

    private suspend fun getPagesList(
        index: Int,
        url: String,
        isSearch: Boolean = false,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(url).toDocument()
                doc.select(
                        if (isSearch) ".c-tabs-item__content .tab-thumb a"
                        else "div.page-item-detail .item-thumb a"
                    )
                    .mapNotNull {
                        BookResult(
                            title = it.attr("title"),
                            url = it.attr("href"),
                            coverImageUrl = it.selectFirst("img")?.attr("data-src") ?: "",
                        )
                    }
                    .let {
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst(".nav-previous") == null,
                        )
                    }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".container#chapter-heding")?.text() ?: ""
        }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".read-container .text-left")!!.let {
                it.select("script").remove()
                TextExtractor.get(it)
            }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".summary_image img")
                    ?.attr("data-src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst(".summary__content")?.let {
                    TextExtractor.get(it)
                }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            // var url = postRequest(url = bookUrl.toUrlBuilderSafe().addPath("ajax", "chapters").toString())
            val postData =
                postRequest(url = bookUrl.toUrlBuilderSafe().addPath("ajax", "chapters").toString())
            tryConnect {
                networkClient
                    .call(postData)
                    .toDocument()
                    .select("li[class=wp-manga-chapter]")
                    .map {
                        it.selectFirst("span")?.remove()
                        ChapterResult(it.text() ?: "", it.selectFirst("a")?.attr("href") ?: "")
                    }
                    .reversed()
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            val page = index + 1
            val url =
                catalogUrl
                    .toUrlBuilderSafe()
                    .ifCase(page > 1) { addPath("page", page.toString()) }
                    .toString()
            getPagesList(index, url)
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            val page = index + 1
            val url =
                baseUrl
                    .toUrlBuilderSafe()
                    .ifCase(page > 1) { addPath("page", page.toString()) }
                    .add("s" to input, "post_type" to "wp-manga")
                    .toString()
            getPagesList(index, url, true)
        }
}
