package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.LanguageCode
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.toDocument
import my.novelreader.network.tryConnect
import my.novelreader.scraper.R
import my.novelreader.scraper.domain.ChapterResult
import my.novelreader.scraper.templates.BaseNovelFullScraper
import org.jsoup.nodes.Document

class NovelHall(
    networkClient: NetworkClient
) : BaseNovelFullScraper(networkClient) {
    override val id = "novelhall"
    override val nameStrId = R.string.source_name_novelhall
    override val baseUrl = "https://www.novelhall.com/"
    override val catalogUrl = "https://www.novelhall.com/all.html"
    override val language = LanguageCode.ENGLISH

    // NovelHall-specific selectors
    override val selectBookCover = ".book-img.hidden-xs img[src]"
    override val selectBookDescription = "span.js-close-wrap"
    override val selectChapterList = "#morelist a[href]"
    override val selectChapterContent = "div#htmlContent"
    override val selectCatalogItems = "li.btm"
    override val selectPaginationLastPage = "div.page-nav span:last-child"
    override val selectSearchItems: String = "td:nth-child(2) a[href]"
    // NovelHall uses direct chapter list, not ajax
    override val useAjaxChapterLoading = false

    override fun buildCatalogUrl(index: Int): String {
        val page = index + 1
        return if (page == 1) "$baseUrl/all.html"
        else "$baseUrl/all-$page.html"
    }

    override fun buildSearchUrl(index: Int, input: String): String {
        return baseUrl + "index.php?s=so&module=book&keyword=$input"
    }
    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            networkClient.get(bookUrl)
                .toDocument()
                .select(selectChapterList)
                .map {
                    ChapterResult(
                        title = it.text() ?: "",
                        url = (baseUrl + it.attr("href"))
                    )
                }
        }
    }

    override fun isLastPage(doc: Document) =
        doc.selectFirst("div.page-nav")?.children()?.last()?.tagName() == "span"
}
