package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
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
import my.novelreader.scraper.templates.BaseNovelFullScraper

class BoxNovel(
    networkClient: NetworkClient
) : BaseNovelFullScraper(networkClient) {
    override val id = "box_novel"
    override val nameStrId = R.string.source_name_box_novel
    override val baseUrl = "https://novlove.com/"
    override val catalogUrl = "https://novlove.com/sort/nov-love-daily-update"
    override val iconUrl = "https://novlove.com/favicon.ico"
    override val language = LanguageCode.ENGLISH

    override val selectCatalogItems = "#list-page div.list-novel .row"
    override val selectCatalogItemTitle = ".novel-title a"
    override val selectCatalogItemCover = "div.col-xs-3 > div > img"

    //override val selectSearchItems: String = "#list-page .row"
    override val selectSearchItemTitle: String = ".novel-title a"
    override val selectSearchItemUrl: String = "a[href]"
    override val selectSearchItemCover: String = "div.col-xs-3 > div > img"
    override val selectPaginationLastPage = "ul.pagination li:last-child"

    override val novelIdSelector = "#rating[data-novel-id]"
    override val ajaxChapterPath = "ajax/chapter-archive"

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val doc = networkClient.get(bookUrl).toDocument()
                doc.selectFirst("meta[itemprop=image]")?.attr("content")
                    ?: doc.selectFirst(".book img")?.attr("src")
            }
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
}
