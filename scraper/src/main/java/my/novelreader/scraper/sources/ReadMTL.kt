package my.novelreader.scraper.sources

import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.templates.BaseMadaraScraper

class ReadMTL(
    networkClient: NetworkClient
) : BaseMadaraScraper(networkClient) {
    override val id = "readmtl"
    override val nameStrId = R.string.source_name_readmtl
    override val baseUrl = "https://www.readmtl.com/"
    override val catalogUrl = "https://www.readmtl.com/novel/?m_orderby=latest"
    override val iconUrl = "https://www.readmtl.com/favicon.ico"
    override val language = LanguageCode.ENGLISH
    
    override val catalogOrderBy = "latest"
}
