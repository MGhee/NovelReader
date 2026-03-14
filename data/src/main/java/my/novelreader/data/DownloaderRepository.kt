package my.novelreader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.Response
import my.novelreader.core.map
import my.novelreader.network.NetworkClient
import my.novelreader.network.toDocument
import my.novelreader.scraper.Scraper
import my.novelreader.scraper.TextExtractor
import my.novelreader.feature.local_database.tables.Chapter
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloaderRepository @Inject constructor(
    private val scraper: Scraper,
    private val networkClient: NetworkClient,
) {

    suspend fun bookCoverImageUrl(
        bookUrl: String,
    ): Response<String?> = withContext(Dispatchers.Default) {
        val error by lazy {
            """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
        }

        // Return if can't find compatible source for url
        val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
            ?: return@withContext Response.Error(error, Exception())

        my.novelreader.network.tryFlatConnect {
            scrap.getBookCoverImageUrl(bookUrl)
        }
    }

    suspend fun bookDescription(
        bookUrl: String,
    ): Response<String?> = withContext(Dispatchers.Default) {
        val error by lazy {
            """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
        }

        // Return if can't find compatible source for url
        val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
            ?: return@withContext Response.Error(error, Exception())

        my.novelreader.network.tryFlatConnect {
            scrap.getBookDescription(bookUrl)
        }
    }

    suspend fun bookChapter(
        chapterUrl: String,
    ): Response<my.novelreader.scraper.ChapterDownload> = withContext(Dispatchers.Default) {
        my.novelreader.network.tryFlatConnect {
            val request = my.novelreader.network.getRequest(chapterUrl)
            val realUrl = networkClient
                .call(request, followRedirects = true)
                .request.url
                .toString()


            val error by lazy {
                """
				Unable to load chapter from url:
				$chapterUrl
				
				Redirect url:
				$realUrl
				
				Source not supported
			""".trimIndent()
            }

            scraper.getCompatibleSource(realUrl)?.also { source ->
                val doc = networkClient.get(source.transformChapterUrl(realUrl)).toDocument(source.charset)
                val data = my.novelreader.scraper.ChapterDownload(
                    body = source.getChapterText(doc) ?: return@also,
                    title = source.getChapterTitle(doc)
                )
                return@tryFlatConnect Response.Success(data)
            }

            // If no predefined source is found try extracting text with heuristic extraction
            val chapter =
                heuristicChapterExtraction(realUrl, networkClient.get(realUrl).toDocument())
            when (chapter) {
                null -> Response.Error(
                    error,
                    Exception("Unable to extract chapter data with heuristics")
                )
                else -> Response.Success(chapter)
            }
        }
    }

    suspend fun bookChaptersList(
        bookUrl: String,
    ): Response<List<Chapter>> = withContext(Dispatchers.Default) {
        val error by lazy {
            """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
        }

        // Return if can't find compatible source for url
        val scrap = scraper.getCompatibleSourceCatalog(bookUrl)
            ?: return@withContext Response.Error(error, Exception())

        my.novelreader.network.tryFlatConnect { scrap.getChapterList(bookUrl) }
            .map { chapters ->
                chapters.mapIndexed { index, it ->
                    Chapter(
                        title = it.title,
                        url = it.url,
                        bookUrl = bookUrl,
                        position = index
                    )
                }
            }
    }
}


private fun heuristicChapterExtraction(url: String, document: Document): my.novelreader.scraper.ChapterDownload? {
    Readability4JExtended(url, document).parse().also { article ->
        val content = article.articleContent ?: return null
        return my.novelreader.scraper.ChapterDownload(
            body = TextExtractor.get(content),
            title = article.title
        )
    }
}
