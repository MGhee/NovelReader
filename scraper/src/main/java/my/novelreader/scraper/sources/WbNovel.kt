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

class WbNovel(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "wbnovel"
    override val nameStrId = R.string.source_name_wb_novel
    override val baseUrl = "https://wbnovel.com/"
    override val catalogUrl = "https://wbnovel.com/all-novel/"
    override val iconUrl =
        "https://wbnovel.com/wp-content/uploads/2019/01/cropped-w-32x32.png"
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
                        if (isSearch) ".tab-content-wrap .c-tabs-item .tab-thumb a"
                        else ".tab-content-wrap .c-tabs-item .item-thumb a"
                    )
                    .mapNotNull {
                        BookResult(
                            title = it.attr("title"),
                            url = it.attr("href"),
                            coverImageUrl = it.selectFirst("img")?.attr("src") ?: "",
                        )
                    }
                    .let {
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst(".nextpostslink") == null,
                        )
                    }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        withContext(Dispatchers.Default) { doc.selectFirst("#chapter-heading")?.text() ?: "" }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".reading-content .text-left")?.let { 
              it.select("div").remove()
              TextExtractor.get(it) 
            } ?: ""
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".profile-manga .summary_image > a img ")
                    ?.attr("src") ?: ""
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".content-area .summary__content p")
                    ?.let { TextExtractor.get(it) }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
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
                    .add("s" to input, "post_type" to "wp-manga", "m_orderby" to "views")
                    .toString()
            getPagesList(index, url, true)
        }
}