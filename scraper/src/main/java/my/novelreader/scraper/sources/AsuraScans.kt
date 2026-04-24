package my.novelreader.scraper.sources

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
import my.novelreader.scraper.sources.asurascans.AsuraChaptersResponse
import my.novelreader.scraper.sources.asurascans.AsuraSearchResponse
import my.novelreader.strings.R
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * AsuraScans - popular source for manhwa/webtoons with high-quality releases.
 * Uses REST API for catalog/search and HTML scraping for chapter images.
 */
class AsuraScans(
    private val networkClient: NetworkClient,
) : MangaSourceInterface {

    override val id: String = "asurascans"
    @StringRes
    override val nameStrId: Int = R.string.source_name_asurascans
    override val baseUrl: String = "https://asurascans.com"
    override val catalogUrl: String = "https://api.asurascans.com/api/series"
    override val language: LanguageCode? = LanguageCode.ENGLISH
    override val readingMode: MangaReadingMode = MangaReadingMode.WEBTOON
    override val direction: MangaDirection = MangaDirection.LTR

    private val apiBaseUrl = "https://api.asurascans.com"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun getChapterImages(chapterUrl: String): Response<List<String>> =
        withContext(Dispatchers.Default) {
            tryConnect("url=$chapterUrl") {
                // Extract slug and chapter number from URL: .../comics/{slug}/chapter/{number}
                val pathParts = chapterUrl.removePrefix(baseUrl).trim('/').split("/")
                // pathParts = ["comics", "{slug}", "chapter", "{number}"]
                val slug = pathParts.getOrNull(1) ?: ""
                val chapterNumber = pathParts.getOrNull(3) ?: ""

                // Strategy 1: Try the API endpoint for chapter detail (most reliable)
                if (slug.isNotBlank() && chapterNumber.isNotBlank()) {
                    val apiImages = tryGetImagesFromApi(slug, chapterNumber)
                    if (apiImages.isNotEmpty()) return@tryConnect apiImages
                }

                // Strategy 2: Parse the HTML page
                val body = networkClient.get(chapterUrl).bodyString()
                val doc = Jsoup.parse(body)

                val webpPattern = Regex("""(https?://[^"'\\\s]+\.webp)""")
                val allWebpUrls = mutableSetOf<String>()

                // From __NEXT_DATA__ JSON
                doc.selectFirst("script#__NEXT_DATA__")?.data()?.let { nextData ->
                    webpPattern.findAll(nextData).forEach { match ->
                        allWebpUrls.add(match.groupValues[1].replace("\\/", "/"))
                    }
                }

                // From img tags
                doc.select("img[src], img[data-src], img[data-lazy-src]").forEach { img ->
                    listOf("src", "data-src", "data-lazy-src").forEach { attr ->
                        val url = img.attr(attr)
                        if (url.startsWith("http") && url.endsWith(".webp")) {
                            allWebpUrls.add(url)
                        }
                    }
                }

                // From all script tags
                doc.select("script").forEach { script ->
                    webpPattern.findAll(script.data()).forEach { match ->
                        allWebpUrls.add(match.groupValues[1].replace("\\/", "/"))
                    }
                }

                // Filter to chapter images only (contain "/chapters/" in the path)
                val chapterWebpUrls = allWebpUrls.filter { it.contains("/chapters/") }
                val urls = chapterWebpUrls.ifEmpty { allWebpUrls }

                // Check if we have numbered filenames (001.webp) — if so, probe for more
                val numberedPattern = Regex("""/(\d+)\.webp$""")
                val numberedUrls = urls.filter { numberedPattern.containsMatchIn(it) }

                if (numberedUrls.isNotEmpty()) {
                    val sampleUrl = numberedUrls.first()
                    val urlBase = sampleUrl.substringBeforeLast("/")
                    val sampleFilename = sampleUrl.substringAfterLast("/").substringBefore(".")
                    val padLength = sampleFilename.length

                    val knownPages = numberedUrls.mapNotNull { url ->
                        url.substringAfterLast("/").substringBefore(".").toIntOrNull()
                    }
                    val maxKnown = knownPages.max()

                    // Probe for pages beyond what the HTML showed
                    val allPages = knownPages.toMutableSet()
                    var page = maxKnown + 1
                    var consecutiveMisses = 0
                    while (consecutiveMisses < 3 && page <= 200) {
                        val pageUrl = "$urlBase/${page.toString().padStart(padLength, '0')}.webp"
                        val exists = try {
                            val headReq = Request.Builder().url(pageUrl).head()
                            val resp = networkClient.call(headReq)
                            resp.close()
                            resp.code in 200..299
                        } catch (e: Exception) {
                            false
                        }
                        if (exists) {
                            allPages.add(page)
                            consecutiveMisses = 0
                        } else {
                            consecutiveMisses++
                        }
                        page++
                    }

                    allPages.sorted().map { p ->
                        "$urlBase/${p.toString().padStart(padLength, '0')}.webp"
                    }
                } else {
                    // Hash-based filenames — return whatever we found, preserving order
                    urls.toList()
                }
            }
        }

    /**
     * Try to get chapter images from the Asura Scans REST API.
     * Returns empty list if the endpoint doesn't exist or doesn't contain images.
     */
    private suspend fun tryGetImagesFromApi(slug: String, chapterNumber: String): List<String> {
        return try {
            val body = networkClient.get("$apiBaseUrl/api/series/$slug/chapters/$chapterNumber").bodyString()
            // Extract all .webp URLs from the JSON response
            val webpPattern = Regex("""(https?://[^"'\\\s]+\.webp)""")
            val images = webpPattern.findAll(body)
                .map { it.groupValues[1].replace("\\/", "/") }
                .distinct()
                .toList()
            // Only return if we got a reasonable number of images
            if (images.size >= 3) images else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("url=$bookUrl") {
                // Extract series slug from bookUrl (format: https://asurascans.com/comics/{slug})
                val slug = bookUrl.substringAfter("/comics/").trimEnd('/')
                if (slug.isBlank()) return@tryConnect emptyList()

                val body = networkClient.get("$apiBaseUrl/api/series/$slug/chapters").bodyString()
                val response = json.decodeFromString<AsuraChaptersResponse>(body)

                response.data
                    .sortedBy { it.number }
                    .mapNotNull { chapter ->
                        val number = chapter.number.toInt()
                        val title = chapter.title?.takeIf { it.isNotBlank() } ?: "Chapter $number"
                        val chapterUrl = "$baseUrl/comics/$slug/chapter/$number"
                        ChapterResult(title, chapterUrl)
                    }
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index") {
                val offset = index * 20
                val body = networkClient.get("$apiBaseUrl/api/series?sort=latest&order=desc&limit=20&offset=$offset").bodyString()
                val response = json.decodeFromString<AsuraSearchResponse>(body)

                val items = response.data.mapNotNull { comic ->
                    val title = comic.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    BookResult(title, "$baseUrl/comics/${comic.slug}", comic.cover ?: "")
                }

                val hasMore = response.meta?.let { meta ->
                    (offset + items.size) < (meta.total ?: 0)
                } ?: false

                PagedList(items, index, !hasMore || items.isEmpty())
            }
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index, input=$input") {
                val body = networkClient.get("$apiBaseUrl/api/search?q=$input").bodyString()
                val response = json.decodeFromString<AsuraSearchResponse>(body)

                val items = response.data.mapNotNull { comic ->
                    val title = comic.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    BookResult(title, "$baseUrl/comics/${comic.slug}", comic.cover ?: "")
                }

                PagedList(items, index, true) // Search results don't have pagination
            }
        }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect("url=$bookUrl") {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)
                doc.selectFirst("img[src*=cdn.asurascans.com/asura-images/covers]")?.attr("src")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect("url=$bookUrl") {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)
                doc.select("p").firstOrNull()?.text()
            }
        }
}
