package my.novelreader.scraper.sources

import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.templates.BaseMadaraScraper

class WuxiaWorld(
    networkClient: NetworkClient
) : BaseMadaraScraper(networkClient) {
    override val id = "wuxia_world"
    override val nameStrId = R.string.source_name_wuxia_world
    override val baseUrl = "https://wuxiaworld.site/"
    override val catalogUrl = "https://wuxiaworld.site/novel/?m_orderby=trending"
    override val iconUrl = "https://wuxiaworld.site/wp-content/uploads/2019/04/favicon-1.ico"
    override val language = LanguageCode.ENGLISH

    override val catalogOrderBy = "alphabet"
    override val selectCatalogItemTitle: String = ".post-title h3 a"
}
