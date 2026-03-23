package my.novelreader.scraper.sources

import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.templates.BaseNovelFullScraper
import org.jsoup.nodes.Document

class NovelBinCom(
    networkClient: NetworkClient
) : BaseNovelFullScraper(networkClient) {
    override val id = "novelbin_com"
    override val nameStrId = R.string.source_name_novelbin_com
    override val baseUrl = "https://novelbin.me/"
    override val catalogUrl = "https://novelbin.me/sort/latest"
    override val iconUrl = "https://novelbin.me/favicon.ico"
    override val language = LanguageCode.ENGLISH

    // Catalog selectors
    override val selectCatalogItems = "#list-page .row"
    override val selectCatalogItemTitle = "h3.novel-title > a"
    override val selectCatalogItemUrl = "h3.novel-title > a"
    override val selectCatalogItemCover = "img.cover"

    // Search selectors (same structure)
    override val selectSearchItems = "#list-page .row"
    override val selectSearchItemTitle = "h3.novel-title > a"
    override val selectSearchItemUrl = "h3.novel-title > a"
    override val selectSearchItemCover = "img.cover"

    // Pagination
    override val selectPaginationLastPage = "ul.pagination li:last-child"

    // Book detail
    override val selectBookCover = ".book img"

    // Chapters are inline, no AJAX needed
    override val useAjaxChapterLoading = false
    override val selectChapterList = "ul.list-chapter li a"

    override fun buildCatalogUrl(index: Int): String {
        val page = index + 1
        return if (page == 1) catalogUrl
        else "$catalogUrl?page=$page"
    }

    override fun buildSearchUrl(index: Int, input: String): String {
        val page = index + 1
        return if (page == 1) "${baseUrl}search?keyword=$input"
        else "${baseUrl}search?keyword=$input&page=$page"
    }

    override fun isLastPage(doc: Document): Boolean {
        val lastLi = doc.selectFirst(selectPaginationLastPage)
        return lastLi == null || lastLi.hasClass("disabled")
    }
}
