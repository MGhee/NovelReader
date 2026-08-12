package my.novelreader.scraper.sources

import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.templates.BaseMadaraScraper

class BoxNovel(
    networkClient: NetworkClient
) : BaseMadaraScraper(networkClient) {
    override val id = "box_novel"
    override val nameStrId = R.string.source_name_box_novel
    override val baseUrl = "https://novelnice.com/"
    override val catalogUrl = "https://novelnice.com/read/?m_orderby=latest"
    override val iconUrl = "https://novelnice.com/favicon.ico"
    override val language = LanguageCode.ENGLISH

    override val catalogPath = "read"
    override val catalogOrderBy = "latest"

    // NovelNice uses src instead of data-src for images
    override val selectBookCover: String = ".summary_image img[src]"
    override val selectCatalogItemCover: String = "img[src]"
    override val selectSearchItemCover: String = "img[src]"
    override val selectCatalogItemTitle: String = ".post-title h3 a"
}
