package my.novelreader.scraper.sources

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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
import my.novelreader.scraper.sources.util.ComixToChapterScraper
import my.novelreader.strings.R
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * Comix.to manga source.
 *
 * Comix.to is a Next.js client-rendered SPA. Catalog and search go through the public JSON
 * API:
 *   • Catalog: /api/v1/manga?order[chapter_updated_at]=desc&limit=28&page={n}
 *   • Search:  /api/v1/manga?keyword={q}&order[relevance]=desc&limit=28&page={n}
 *
 * The chapters endpoint is gated by a JS-set session cookie we can't replicate from a plain
 * HTTP client, so the chapter list is scraped from the rendered title page via WebView (see
 * [ComixToChapterScraper]). Chapter image extraction stays static — chapter reader pages
 * render their <img> tags server-side.
 */
class ComixTo(
    private val networkClient: NetworkClient,
    appContext: Context,
) : MangaSourceInterface {

    private val chapterScraper = ComixToChapterScraper(appContext)
    private val chapterApiUrlCache = ConcurrentHashMap<String, String>()

    override val id: String = "comixto"

    @StringRes
    override val nameStrId: Int = R.string.source_comixto

    override val baseUrl: String = "https://comix.to"
    override val catalogUrl: String = "$baseUrl/browser?genres_mode=and"
    override val language: LanguageCode? = LanguageCode.ENGLISH
    override val readingMode: MangaReadingMode = MangaReadingMode.WEBTOON
    override val direction: MangaDirection = MangaDirection.LTR

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private companion object {
        const val TAG = "ComixTo"
        const val API_BASE = "https://comix.to/api/v1"
        const val CATALOG_PAGE_SIZE = 28
        const val CHAPTERS_PAGE_SIZE = 20
        const val MAX_PAGE_PROBE = 200
        const val MAX_CONSECUTIVE_IMAGE_MISSES = 5
    }

    private data class HtmlChapterItem(
        val url: String,
        val rawTitle: String,
        val groupName: String,
        val number: Double,
    )

    @Serializable
    private data class CatalogEnvelope(
        val status: String? = null,
        val result: CatalogResult? = null,
    )

    @Serializable
    private data class CatalogResult(
        val items: List<CatalogItem> = emptyList(),
        val pagination: Pagination? = null,
        val meta: CatalogMeta? = null,
    )

    @Serializable
    private data class CatalogItem(
        val manga_id: Long = 0,
        val id: Long = 0,
        val hash_id: String = "",
        val hid: String = "",
        val title: String = "",
        val slug: String = "",
        val url: String = "",
        val poster: Poster? = null,
        val synopsis: String? = null,
    )

    @Serializable
    private data class Poster(
        val small: String? = null,
        val medium: String? = null,
        val large: String? = null,
    )

    @Serializable
    private data class Pagination(
        val current_page: Int = 1,
        val last_page: Int = 1,
        val total: Int = 0,
    )

    @Serializable
    private data class CatalogMeta(
        val total: Int = 0,
        val perPage: Int = CATALOG_PAGE_SIZE,
        val page: Int = 1,
        val lastPage: Int = 1,
        val hasNext: Boolean = false,
        val hasPrev: Boolean = false,
    )

    @Serializable
    private data class ChapterEnvelope(val result: ChapterApiResult? = null)

    @Serializable
    private data class ChapterApiResult(
        val items: List<ChapterApiItem> = emptyList(),
        val pagination: ChapterPagination? = null,
    )

    @Serializable
    private data class ChapterApiItem(
        val chapter_id: Long = 0,
        val number: Double = 0.0,
        val name: String = "",
        val scanlation_group: ChapterGroup? = null,
    )

    @Serializable
    private data class ChapterGroup(
        val name: String = "",
    )

    @Serializable
    private data class ChapterPagination(
        val current_page: Int = 1,
        val last_page: Int = 1,
    )

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index") {
                fetchCatalog(
                    url = "$API_BASE/manga?order%5Bchapter_updated_at%5D=desc" +
                        "&limit=$CATALOG_PAGE_SIZE&page=${index + 1}",
                    pageIndex = index,
                )
            }
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index, input=$input") {
            if (input.isBlank()) return@tryConnect PagedList.createEmpty(index)
            val q = URLEncoder.encode(input, "UTF-8")
            fetchCatalog(
                url = "$API_BASE/manga?keyword=$q&order%5Brelevance%5D=desc" +
                    "&limit=$CATALOG_PAGE_SIZE&page=${index + 1}",
                pageIndex = index,
            )
        }
    }

    private suspend fun fetchCatalog(url: String, pageIndex: Int): PagedList<BookResult> {
        val body = networkClient.get(url).bodyString()
        val envelope = json.decodeFromString<CatalogEnvelope>(body)
        val result = envelope.result ?: return PagedList.createEmpty(pageIndex)

        val books = result.items.mapNotNull { it.toBookResult() }
        val pagination = result.pagination
        val meta = result.meta
        val isLastPage = books.isEmpty() ||
            (meta?.hasNext == false) ||
            (meta != null && meta.page >= meta.lastPage) ||
            (pagination != null && pagination.current_page >= pagination.last_page)
        return PagedList(books, pageIndex, isLastPage = isLastPage)
    }

    private fun CatalogItem.toBookResult(): BookResult? {
        if (title.isBlank()) return null
        val bookUrl = when {
            url.startsWith("http") -> url
            hid.isNotBlank() -> "$baseUrl/title/$hid"
            hash_id.isNotBlank() && slug.isNotBlank() -> "$baseUrl/title/$hash_id-$slug"
            else -> return null
        }
        val cover = poster?.medium ?: poster?.large ?: poster?.small ?: ""
        return BookResult(
            title = title,
            url = bookUrl,
            coverImageUrl = cover,
            description = synopsis.orEmpty(),
        )
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect("url=$bookUrl") {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)
                doc.selectFirst("meta[property=og:image][content]")?.attr("content")
                    ?.takeIf { it.isNotBlank() }
                    ?: doc.selectFirst("meta[name=twitter:image][content]")?.attr("content")
                        ?.takeIf { it.isNotBlank() }
                    ?: doc.selectFirst("img[itemProp=image][src]")?.absUrl("src")
                        ?.takeIf { it.isNotBlank() }
                    ?: doc.selectFirst("img[src*=static.comix.to]")?.absUrl("src")
                        ?.takeIf { it.isNotBlank() }
                    ?: Regex("""https://static\.comix\.to/[^\"'\s>]+""")
                        .find(body)
                        ?.value
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            tryConnect("url=$bookUrl") {
                val body = networkClient.get(bookUrl).bodyString()
                val doc = Jsoup.parse(body)
                doc.selectFirst("[itemProp=description]")?.text()
                    ?: doc.selectFirst("meta[name=description]")?.attr("content")
                    ?: doc.select("p").firstOrNull { it.text().length > 80 }?.text()
            }
        }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        tryConnect("url=$bookUrl") {
            Log.i(TAG, "getChapterList bookUrl=$bookUrl")
            loadChapterListViaHtml(bookUrl)
                ?.also { return@tryConnect it }

            val titleSegment = bookUrl.substringAfter("/title/")
                .substringBefore("/")
                .substringBefore("?")
            if (titleSegment.isBlank()) return@tryConnect emptyList()

            chapterApiUrlCache[titleSegment]?.let { cachedApiUrl ->
                loadChapterListViaApi(bookUrl, titleSegment, cachedApiUrl, fromCache = true)
                    ?.also { return@tryConnect it }
                chapterApiUrlCache.remove(titleSegment, cachedApiUrl)
            }

            chapterScraper.discoverChapterApiUrl(bookUrl)?.let { apiUrl ->
                loadChapterListViaApi(bookUrl, titleSegment, apiUrl, fromCache = false)
                    ?.also {
                        chapterApiUrlCache[titleSegment] = apiUrl
                        return@tryConnect it
                    }
            }

            // Fallback: scrape chapters out of the rendered title page via a hidden WebView
            // if we fail to discover and reuse the real chapters API endpoint.
            val scraped = chapterScraper.extract(bookUrl)
            Log.i(TAG, "scraped ${scraped.size} chapters from $bookUrl")

            scraped
                .distinctBy { it.url.substringBefore("?") }
                .sortedWith(
                    compareBy<ComixToChapterScraper.ScrapedChapter> {
                        it.groupName.ifBlank { "~" }.lowercase()
                    }.thenBy {
                        extractChapterNumber(it.url)
                    }.thenBy {
                        it.rawTitle.lowercase()
                    }
                )
                .map { item ->
                    val absolute = if (item.url.startsWith("http")) item.url else "$baseUrl${item.url}"
                    val number = extractChapterNumber(absolute)
                    val numStr = if (number > 0.0) formatNumber(number) else absolute.substringAfterLast('/')
                    val rawTitle = item.rawTitle.ifBlank { "Ch. $numStr" }
                    val title = if (item.groupName.isNotBlank()) "$rawTitle — ${item.groupName}" else rawTitle
                    ChapterResult(title = title, url = absolute)
                }
        }

    private suspend fun loadChapterListViaHtml(bookUrl: String): List<ChapterResult>? = runCatching {
        fetchChapterListViaHtml(bookUrl)
    }.onFailure {
        Log.w(TAG, "html chapter scrape failed for $bookUrl", it)
    }.getOrNull()?.takeIf { it.isNotEmpty() }?.also { chapters ->
        Log.i(TAG, "loaded ${chapters.size} chapters via html for $bookUrl")
    }

    private suspend fun fetchChapterListViaHtml(bookUrl: String): List<ChapterResult> {
        val normalizedBookUrl = bookUrl.substringBefore('?')
        val firstDoc = Jsoup.parse(networkClient.get(normalizedBookUrl).bodyString())
        val totalPages = readHtmlChapterPages(firstDoc)
        val remainingDocs = coroutineScope {
            (2..totalPages).map { page ->
                async {
                    Jsoup.parse(networkClient.get(withPage(normalizedBookUrl, page)).bodyString())
                }
            }.awaitAll()
        }

        return buildList {
            addAll(parseHtmlChapterItems(firstDoc))
            remainingDocs.forEach { addAll(parseHtmlChapterItems(it)) }
        }
            .distinctBy { it.url.substringBefore('?') }
            .sortedWith(
                compareBy<HtmlChapterItem> {
                    it.groupName.ifBlank { "~" }.lowercase()
                }.thenBy {
                    it.number
                }.thenBy {
                    it.rawTitle.lowercase()
                }
            )
            .map { item ->
                val rawTitle = item.rawTitle.ifBlank {
                    val numStr = if (item.number > 0.0) formatNumber(item.number) else item.url.substringAfterLast('/')
                    "Ch. $numStr"
                }
                val title = if (item.groupName.isNotBlank()) "$rawTitle — ${item.groupName}" else rawTitle
                ChapterResult(title = title, url = item.url)
            }
    }

    private fun readHtmlChapterPages(doc: Document): Int {
        val text = doc.text()
        val totalItems = Regex("""Showing\s+\d+\s+to\s+\d+\s+of\s+([\d,]+)\s+items""")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toIntOrNull()
            ?: return 1
        return ceil(totalItems / CHAPTERS_PAGE_SIZE.toDouble()).toInt().coerceAtLeast(1)
    }

    private fun parseHtmlChapterItems(doc: Document): List<HtmlChapterItem> =
        doc.select("a[href*=/title/][href*=-chapter-]")
            .mapNotNull { anchor ->
                val href = anchor.absUrl("href").ifBlank { anchor.attr("href") }
                if (href.isBlank()) return@mapNotNull null

                val rawTitle = anchor.text().trim()
                val groupName = anchor.findNearestGroupName()
                HtmlChapterItem(
                    url = href,
                    rawTitle = rawTitle,
                    groupName = groupName,
                    number = extractChapterNumber(href),
                )
            }

    private fun Element.findNearestGroupName(): String {
        var current: Element? = parent()
        repeat(6) {
            val group = current
                ?.select("a[href*=/groups/]")
                ?.firstOrNull()
                ?.text()
                ?.trim()
                .orEmpty()
            if (group.isNotBlank()) return group
            current = current?.parent()
        }

        var sibling: Element? = nextElementSibling()
        repeat(4) {
            val group = sibling?.takeIf { it.attr("href").contains("/groups/") }
                ?.text()
                ?.trim()
                .orEmpty()
            if (group.isNotBlank()) return group
            sibling = sibling?.nextElementSibling()
        }
        return ""
    }

    private fun withPage(bookUrl: String, page: Int): String =
        if ('?' in bookUrl) "$bookUrl&page=$page" else "$bookUrl?page=$page"

    private suspend fun loadChapterListViaApi(
        bookUrl: String,
        titleSegment: String,
        firstPageApiUrl: String,
        fromCache: Boolean,
    ): List<ChapterResult>? = runCatching {
        fetchChapterListViaApi(
            bookUrl = bookUrl,
            titleSegment = titleSegment,
            firstPageApiUrl = firstPageApiUrl,
        )
    }.onFailure {
        Log.w(TAG, "chapter api ${if (fromCache) "cache" else "discovery"} failed for $bookUrl", it)
    }.getOrNull()?.takeIf { it.isNotEmpty() }?.also { chapters ->
        Log.i(TAG, "loaded ${chapters.size} chapters via api for $bookUrl")
    }

    private suspend fun fetchChapterListViaApi(
        bookUrl: String,
        titleSegment: String,
        firstPageApiUrl: String,
    ): List<ChapterResult> {
        val firstPage = decodeChapterPage(firstPageApiUrl).result ?: return emptyList()
        val lastPage = firstPage.pagination?.last_page ?: 1
        val remainingPages = coroutineScope {
            (2..lastPage).map { page ->
                async {
                    decodeChapterPage(replacePageParam(firstPageApiUrl, page)).result
                }
            }.awaitAll()
        }
        val pages = buildList {
            add(firstPage)
            addAll(remainingPages.filterNotNull())
        }

        return pages
            .asSequence()
            .flatMap { it.items.asSequence() }
            .sortedWith(
                compareBy<ChapterApiItem> {
                    it.scanlation_group?.name.orEmpty().ifBlank { "~" }.lowercase()
                }.thenBy {
                    it.number
                }.thenBy {
                    it.name.lowercase()
                }
            )
            .mapNotNull { it.toChapterResult(titleSegment) }
            .distinctBy { it.url.substringBefore("?") }
            .toList()
            .also { Log.i(TAG, "fetched ${it.size} chapters from api for $bookUrl") }
    }

    private suspend fun decodeChapterPage(url: String): ChapterEnvelope =
        json.decodeFromString(networkClient.get(url).bodyString())

    private fun replacePageParam(url: String, page: Int): String =
        url.replace(Regex("([?&]page=)\\d+"), "$1$page")

    private fun ChapterApiItem.toChapterResult(titleSegment: String): ChapterResult? {
        if (chapter_id <= 0L || number <= 0.0) return null
        val numberStr = formatNumber(number)
        val rawTitle = name.ifBlank { "Ch. $numberStr" }
        val groupName = scanlation_group?.name.orEmpty()
        val title = if (groupName.isNotBlank()) "$rawTitle — $groupName" else rawTitle
        return ChapterResult(
            title = title,
            url = "$baseUrl/title/$titleSegment/$chapter_id-chapter-$numberStr",
        )
    }

    private fun extractChapterNumber(url: String): Double {
        val match = Regex("""-chapter-(\d+(?:\.\d+)?)""").find(url)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun formatNumber(n: Double): String =
        if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()

    override suspend fun getChapterImages(chapterUrl: String): Response<List<String>> =
        tryConnect("url=$chapterUrl") {
            // The chapter reader is Next.js client-rendered, so we use a WebView only to
            // discover the image URL pattern and, when available, the exact total page
            // count. We can then construct the full list directly over plain HTTP instead
            // of waiting for the WebView to lazy-load every page.
            val discovered = chapterScraper.discoverChapterImages(chapterUrl)
                ?: return@tryConnect emptyList()
            val pattern = imagePatternOrNull(discovered.firstImageUrl)
                ?: return@tryConnect listOf(discovered.firstImageUrl)

            discovered.totalPages?.let { totalPages ->
                val generated = (pattern.startIndex until (pattern.startIndex + totalPages))
                    .map(pattern::urlFor)
                Log.i(TAG, "generated $totalPages images from chapter counter for $chapterUrl")
                return@tryConnect generated
            }

            val images = mutableListOf<String>()
            var n = pattern.startIndex
            var consecutiveMisses = 0
            while (n - pattern.startIndex < MAX_PAGE_PROBE && consecutiveMisses < MAX_CONSECUTIVE_IMAGE_MISSES) {
                val url = pattern.urlFor(n)
                if (urlExists(url)) {
                    images += url
                    consecutiveMisses = 0
                } else {
                    consecutiveMisses++
                }
                n++
            }
            Log.i(TAG, "probed ${images.size} images for $chapterUrl")
            images
        }

    private data class ImagePattern(
        val prefix: String,        // e.g. https://jloo.wowpic1.store/ii/{key}/
        val padding: Int,          // length of the numeric segment (e.g. 2 for "01")
        val suffix: String,        // e.g. .webp
        val startIndex: Int,       // first numeric index, usually 1
    ) {
        fun urlFor(n: Int): String =
            "$prefix${n.toString().padStart(padding, '0')}$suffix"
    }

    private fun imagePatternOrNull(firstUrl: String): ImagePattern? {
        val match = Regex("""^(.+?/)(\d+)(\.[A-Za-z0-9]+)(?:[?#].*)?$""").find(firstUrl)
            ?: return null
        val (prefix, num, suffix) = match.destructured
        return ImagePattern(
            prefix = prefix,
            padding = num.length,
            suffix = suffix,
            startIndex = num.toInt().coerceAtLeast(1),
        )
    }

    private suspend fun urlExists(url: String): Boolean = runCatching {
        val referer = runCatching {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        }.getOrDefault(baseUrl)

        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .header("Referer", referer)

        val response = networkClient.call(request, followRedirects = true)
        val ok = response.isSuccessful
        response.close()
        ok
    }.getOrDefault(false)

    override suspend fun latest(page: Int): Response<PagedList<BookResult>> =
        getCatalogList(page - 1)

    override suspend fun popular(page: Int): Response<PagedList<BookResult>> =
        getCatalogList(page - 1)
}
