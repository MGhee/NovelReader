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
import my.novelreader.network.toDocument
import my.novelreader.network.toUrlBuilderSafe
import my.novelreader.network.tryConnect
import my.novelreader.scraper.R
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.TextExtractor
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult
import org.jsoup.nodes.Document

class SakuraNovel(private val networkClient: NetworkClient) : SourceInterface.Catalog {
    override val id = "sakura_novel"
    override val nameStrId = R.string.source_name_sakura_novel
    override val baseUrl = "https://sakuranovel.id/"
    override val catalogUrl = "https://sakuranovel.id/series/"
    override val iconUrl =
        "https://sakuranovel.id/wp-content/uploads/2023/07/cropped-cropped-Icon-1-32x32.png"
    override val language = LanguageCode.INDONESIAN

    private suspend fun getPagesList(
        index: Int,
        url: String,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(url).toDocument()
                doc.select(".flexbox2-item > .flexbox2-content")
                    .mapNotNull {
                        val link = it.selectFirst("a") ?: return@mapNotNull null
                        val bookCover = it.selectFirst(".flexbox2-thumb > img")?.attr("src") ?: ""
                        BookResult(
                            title = link.attr("title"),
                            url = link.attr("href"),
                            coverImageUrl = bookCover,
                        )
                    }
                    .let {
                        PagedList(
                            list = it,
                            index = index,
                            isLastPage = doc.selectFirst("div.pagination .next") == null,
                        )
                    }
            }
        }

    override suspend fun getChapterTitle(doc: Document): String =
        withContext(Dispatchers.Default) { doc.selectFirst("h2 > .title-chapter")?.text() ?: "" }

    override suspend fun getChapterText(doc: Document): String =
        withContext(Dispatchers.Default) {
            doc.selectFirst(".container .asdasd")!!.let { TextExtractor.get(it) }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .selectFirst(".series-thumb:has(img) img")
                    ?.attr("src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient.get(bookUrl).toDocument().selectFirst(".series-synops")?.let {
                    TextExtractor.get(it)
                }
            }
        }

    override suspend fun getChapterList(bookUrl: String) =
        withContext(Dispatchers.Default) {
            tryConnect {
                networkClient
                    .get(bookUrl)
                    .toDocument()
                    .select(".series-chapterlists .flexch-infoz a")
                    .map { ChapterResult(it.attr("title") ?: "", it.attr("href") ?: "") }
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
                    .add("s" to input)
                    .toString()
            getPagesList(index, url)
        }
}
