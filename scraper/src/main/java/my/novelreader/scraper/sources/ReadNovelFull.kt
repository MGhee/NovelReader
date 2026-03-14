package my.novelreader.scraper.sources

import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.templates.BaseNovelFullScraper
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * https://readnovelfull.com/reincarnation-of-the-strongest-sword-god-v2.html
 * Chapter url example:
 * https://readnovelfull.com/reincarnation-of-the-strongest-sword-god/chapter-1-starting-over-v1.html
 */
class ReadNovelFull(
    networkClient: NetworkClient
) : BaseNovelFullScraper(networkClient) {
    override val id = "read_novel_full"
    override val nameStrId = R.string.source_name_read_novel_full
    override val baseUrl = "https://readnovelfull.com/"
    override val catalogUrl = "https://readnovelfull.com/novel-list/most-popular-novel"
    override val language = LanguageCode.ENGLISH

    // ReadNovelFull-specific catalog structure
    override val selectCatalogItems = ".list-novel .row"
    override val selectCatalogItemTitle = ".novel-title a"
    override val selectCatalogItemCover = "div.col-xs-3 > div > img"

    override val selectSearchItems: String = "#list-page div.list-novel .row"
    override val selectSearchItemTitle: String = ".novel-title a"
    override val selectSearchItemUrl: String = "a[href]"
    override val selectSearchItemCover: String = "div.col-xs-3 > div > img"
    override val selectPaginationLastPage = "ul.pagination li:last-child"

    // Specific ajax settings
    override val novelIdSelector = "#rating[data-novel-id]"
    override val ajaxChapterPath = "ajax/chapter-archive"

    override fun isLastPage(doc: Document): Boolean {
        val lastLi = doc.selectFirst(selectPaginationLastPage)
        return lastLi == null || lastLi.hasClass("disabled")
    }
}
