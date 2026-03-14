package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.scraper.R
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.TextExtractor
import org.jsoup.nodes.Document

class Sousetsuka: SourceInterface.Base {
    override val id = "sousetsuka"
    override val nameStrId = R.string.source_name_sousetsuka
    override val baseUrl = "https://www.sousetsuka.com/"

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            doc.title().ifBlank { null }
        }

    override suspend fun getChapterText(doc: Document): String = withContext(Dispatchers.Default) {
        doc.selectFirst(".post-body.entry-content")!!
            .let { TextExtractor.get(it) }
    }
}