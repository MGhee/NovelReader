package my.novelreader.scraper.sources

import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.templates.BaseNovelFullScraper

class NovelBinCom(
    networkClient: NetworkClient
) : BaseNovelFullScraper(networkClient) {
    override val id = "novelbin_com"
    override val nameStrId = R.string.source_name_novelbin_com
    override val baseUrl = "https://novelbin.com/"
    override val catalogUrl = "https://novelbin.com/latest-release-novel"
    override val iconUrl = "https://novelbin.com/favicon.ico"
    override val language = LanguageCode.ENGLISH
    
    override val novelIdSelector = "#rating[data-novel-id]"
    override val ajaxChapterPath = "ajax-chapter-option"
    
    override fun buildCatalogUrl(index: Int): String {
        val page = index + 1
        return if (page == 1) catalogUrl
        else "$catalogUrl?page=$page"
    }
    
    override fun buildSearchUrl(index: Int, input: String): String {
        val page = index + 1
        return if (page == 1) "$baseUrl/search?keyword=$input"
        else "$baseUrl/search?keyword=$input&page=$page"
    }
}
