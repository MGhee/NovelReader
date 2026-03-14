package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.LanguageCode
import my.novelreader.network.NetworkClient
import my.novelreader.scraper.R
import my.novelreader.scraper.TextExtractor
import my.novelreader.scraper.templates.BaseMadaraScraper
import org.jsoup.nodes.Document

class MoreNovel(
    networkClient: NetworkClient
) : BaseMadaraScraper(networkClient) {
    override val id = "more_webnovel"
    override val nameStrId = R.string.source_name_more_novel
    override val baseUrl = "https://morenovel.net/"
    override val catalogUrl = "https://morenovel.net/novel/?m_orderby=views"
    override val iconUrl = "https://morenovel.net/wp-content/uploads/2020/03/cropped-m2-32x32.png"
    override val language = LanguageCode.INDONESIAN
    
    override val catalogOrderBy = "views"
    
    // MoreNovel-specific selectors
    override val selectBookCover = ".profile-manga .summary_image img"
    override val selectBookDescription = ".content-area .summary__content p"
    override val selectChapterContent = ".reading-content .text-left"
    override val selectCatalogItems = ".tab-content-wrap .c-tabs-item .item-thumb a"
    override val selectSearchItems = ".tab-content-wrap .c-tabs-item .tab-thumb a"
    
    override suspend fun getChapterTitle(doc: Document): String = 
        doc.selectFirst("#chapter-heading")?.text() ?: ""
    
    override suspend fun getChapterText(doc: Document): String = 
        withContext(Dispatchers.Default) {
            doc.selectFirst(".reading-content .text-left")?.let {
                it.select("div").remove()
                TextExtractor.get(it)
            } ?: ""
        }
}
