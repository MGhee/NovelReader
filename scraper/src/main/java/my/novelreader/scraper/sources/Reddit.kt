package my.novelreader.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.network.toUrlBuilder
import my.novelreader.scraper.R
import my.novelreader.scraper.SourceInterface
import my.novelreader.scraper.TextExtractor
import org.jsoup.nodes.Document

/**
 * Novel main page (chapter list) example:
 * Doesn't have main page
 * Chapter url example: (redirected)
 * https://www.reddit.com/r/mushokutensei/comments/g50ry7/translation_old_dragons_tale_chapter_1_dragon_and/
 */
class Reddit : SourceInterface.Base {
    override val id = "reddit"
    override val nameStrId = R.string.source_name_reddit
    override val baseUrl = "https://www.reddit.com/"

    override suspend fun transformChapterUrl(
        url: String
    ): String = withContext(Dispatchers.Default) {
        url.toUrlBuilder()!!.authority("old.reddit.com").toString()
    }

    override suspend fun getChapterTitle(
        doc: Document
    ): String? = withContext(Dispatchers.Default) {
        doc.title().ifBlank { null }
    }

    override suspend fun getChapterText(
        doc: Document
    ): String = withContext(Dispatchers.Default) {
        doc.selectFirst(".linklisting")!!
            .selectFirst(".usertext-body, .may-blank-within, .md-container")!!
            .let {
                it.select("table").remove()
                it.select("blockquote").remove()
                TextExtractor.get(it)
            }
    }
}