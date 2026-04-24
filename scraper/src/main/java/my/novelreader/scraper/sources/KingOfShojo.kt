package my.novelreader.scraper.sources

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.LanguageCode
import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.bodyString
import my.novelreader.network.tryConnect
import my.novelreader.scraper.MangaDirection
import my.novelreader.scraper.MangaReadingMode
import my.novelreader.scraper.MangaSourceInterface
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult
import my.novelreader.strings.R
import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * King of Shojo manga source.
 *
 * Base URL: https://kingofshojo.com
 * Book URLs:    https://kingofshojo.com/manga/[series-slug]/
 * Chapter URLs: https://kingofshojo.com/[series-slug]-chapter-[number]/
 */
class KingOfShojo(
    private val networkClient: NetworkClient
) : MangaSourceInterface {
    override val id: String = "kingofshojo"

    @StringRes
    override val nameStrId: Int = R.string.source_kingofshojo

    override val baseUrl: String = "https://kingofshojo.com"
    override val catalogUrl: String = "$baseUrl/manga/"
    override val language: LanguageCode? = LanguageCode.ENGLISH

    override val readingMode: MangaReadingMode = MangaReadingMode.WEBTOON
    override val direction: MangaDirection = MangaDirection.LTR

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)

                // Try to find cover image in various possible locations
                doc.selectFirst("img[class*=series-cover]")?.attr("src")
                    ?: doc.selectFirst("img[alt*=cover]")?.attr("src")
                    ?: doc.selectFirst(".post-image img")?.attr("src")
                    ?: doc.selectFirst("img[src*=wp-content/uploads]")?.attr("src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)

                // Try to find description in various possible locations
                doc.selectFirst(".manga-description")?.text()
                    ?: doc.selectFirst("[class*=description]")?.text()
                    ?: doc.selectFirst("[class*=synopsis]")?.text()
            }
        }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)

                val chapters = mutableListOf<ChapterResult>()

                // Parse chapter list - chapters are <a> tags linking to chapter pages
                // Look for links that contain "chapter" in the href
                doc.select("a[href*=chapter]").forEach { element ->
                    val chapterUrl = element.attr("href").takeIf { it.isNotBlank() } ?: return@forEach
                    val chapterTitle = element.text().takeIf { it.isNotBlank() } ?: return@forEach

                    if (chapterUrl.startsWith("http") && "chapter" in chapterUrl.lowercase()) {
                        chapters.add(
                            ChapterResult(
                                title = chapterTitle.trim(),
                                url = chapterUrl
                            )
                        )
                    }
                }

                chapters
            }
        }

    override suspend fun getChapterImages(chapterUrl: String): Response<List<String>> =
        withContext(Dispatchers.Default) {
            tryConnect {
                val body = networkClient.get(chapterUrl).bodyString()
                val doc = Jsoup.parse(body)

                // Find the ts_reader initialization which contains image URLs
                val scriptContent = doc.select("script").firstOrNull {
                    it.data().contains("ts_reader.run") || it.data().contains("\"images\"")
                }?.data() ?: return@tryConnect emptyList()

                // Extract images array from ts_reader.run({..., "images":[...], ...})
                val images = mutableListOf<String>()

                // Parse image URLs from the images array in ts_reader.run()
                // Pattern: "images":["url1","url2",...]
                val imagesArrayMatch = "\"images\"\\s*:\\s*\\[(.*?)\\]".toRegex().find(scriptContent)
                if (imagesArrayMatch != null) {
                    val arrayContent = imagesArrayMatch.groupValues[1]
                    // Extract quoted URLs
                    val urlPattern = "\"([^\"]*(?:cdn\\.kingofshojo\\.com|ibb\\.co)[^\"]*?)\"".toRegex()
                    urlPattern.findAll(arrayContent).forEach { match ->
                        val url = match.groupValues[1]
                            .replace("\\/", "/")
                            .trim()
                        if (url.isNotBlank()) {
                            images.add(url)
                        }
                    }
                }

                // Also try to extract direct img tags from the page
                if (images.isEmpty()) {
                    doc.select("img[src*=cdn.kingofshojo.com], img[src*=ibb.co]").forEach { img ->
                        val src = img.attr("src").takeIf { it.isNotBlank() } ?: return@forEach
                        if (src !in images) {
                            images.add(src)
                        }
                    }
                }

                images
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index") {
                val page = index + 1
                val body = networkClient.get("$baseUrl/manga/?page=$page").bodyString()
                val doc = Jsoup.parse(body)

                val books = doc.select("a[href*=/manga/]").mapNotNull { link ->
                    val url = link.attr("href").takeIf { it.isNotBlank() && it.contains("/manga/") } ?: return@mapNotNull null

                    // Skip links that are just to /manga/ page itself
                    if (url == "$baseUrl/manga/" || url == "$baseUrl/manga") return@mapNotNull null

                    // Only keep links with images (actual manga entries have thumbnails)
                    val imgElement = link.selectFirst("img") ?: return@mapNotNull null
                    val coverImageUrl = imgElement.attr("src").takeIf { it.isNotBlank() } ?: return@mapNotNull null

                    val title = link.attr("title").takeIf { it.isNotBlank() }
                        ?: link.text().takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                    BookResult(
                        title = title.trim(),
                        url = url,
                        coverImageUrl = coverImageUrl
                    )
                }.distinctBy { it.url }

                // Check for next page link to determine if this is the last page
                val hasNextPage = doc.selectFirst("a.next, a[rel=next], .nav-next a, .pagination .next, a[href*=page=${page + 1}]") != null
                PagedList(
                    list = books,
                    index = index,
                    isLastPage = !hasNextPage || books.isEmpty()
                )
            }
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank()) return@tryConnect PagedList.createEmpty(index)

            val page = index + 1
            val encodedQuery = URLEncoder.encode(input, "UTF-8")
            val body = networkClient.get("$baseUrl/?s=$encodedQuery&paged=$page").bodyString()
            val doc = Jsoup.parse(body)

            val books = doc.select("a[href*=/manga/]").mapNotNull { link ->
                val url = link.attr("href").takeIf { it.isNotBlank() && it.contains("/manga/") } ?: return@mapNotNull null

                // Skip links that are just to /manga/ page itself
                if (url == "$baseUrl/manga/" || url == "$baseUrl/manga") return@mapNotNull null

                // Only keep links with images (actual manga entries have thumbnails)
                val imgElement = link.selectFirst("img") ?: return@mapNotNull null
                val coverImageUrl = imgElement.attr("src").takeIf { it.isNotBlank() } ?: return@mapNotNull null

                val title = link.attr("title").takeIf { it.isNotBlank() }
                    ?: link.text().takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                BookResult(
                    title = title.trim(),
                    url = url,
                    coverImageUrl = coverImageUrl
                )
            }.distinctBy { it.url }

            // Check for next page link to determine if this is the last page
            val hasNextPage = doc.selectFirst("a.next, a[rel=next], .nav-next a, .pagination .next") != null
            PagedList(
                list = books,
                index = index,
                isLastPage = !hasNextPage || books.isEmpty()
            )
        }
    }

    override suspend fun latest(page: Int): Response<PagedList<BookResult>> =
        getCatalogList(page - 1)

    override suspend fun popular(page: Int): Response<PagedList<BookResult>> =
        getCatalogList(page - 1)
}
