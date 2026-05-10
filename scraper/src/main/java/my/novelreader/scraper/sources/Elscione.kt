package my.novelreader.scraper.sources

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import my.novelreader.core.AppFileResolver
import my.novelreader.core.ChapterDownloadProgress
import my.novelreader.core.LanguageCode
import my.novelreader.core.PagedList
import my.novelreader.core.Response
import my.novelreader.epub_tooling.EpubBook
import my.novelreader.epub_tooling.epubParser
import my.novelreader.network.NetworkClient
import my.novelreader.network.bodyString
import my.novelreader.network.postRequest
import my.novelreader.network.tryConnect
import my.novelreader.scraper.LightNovelSourceInterface
import my.novelreader.scraper.domain.BookResult
import my.novelreader.scraper.domain.ChapterResult
import my.novelreader.strings.R
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.nodes.Document
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Elscione (server.elscione.com) — h5ai 0.29 directory of officially translated light novels.
 *
 * The site is a JS file browser; we talk to its JSON API directly:
 *   POST /_h5ai/public/index.php
 *   body: action=get&items[href]=<path>&items[what]=N   (N=1 immediate, 2 recursive)
 *
 * Response items have:
 *   - folders: href ends with "/", with managed/fetched/size:null
 *   - files:   href does not end with "/", with numeric size + time
 *
 * Mapping:
 *   - Top-level folder under [CATALOG_ROOT_PATH] = a Book. Categorization folders whose
 *     name starts with `!` are filtered out — they're curation buckets, not actual series.
 *   - Each .epub file inside the book's folder tree = a Chapter, sorted in natural order so
 *     `Volume 2` precedes `Volume 10`. Chapter listing uses what=1 with bounded client-side
 *     recursion to keep individual API responses small and resilient to slow subtrees.
 *   - PDFs are filtered out (the app has no PDF reader). PDF-only series surface a single
 *     placeholder chapter explaining why the book appears empty.
 *   - Chapter body is the EPUB's full content as parsed by [epubParser]. Internal images
 *     are written to the same on-disk location the local-EPUB importer uses; the reader's
 *     `AppFileResolver` then renders them via the standard local-image path. Each internal
 *     EPUB chapter's title is prepended to its body so the reader shows the volume's
 *     headings (the reader's own `ReaderItem.Title` only carries the volume-level name).
 *     Image markers whose file we can't extract (SVG, etc.) become a plain-text scene-break
 *     so they don't render as Glide's error placeholder.
 */
class Elscione(
    private val networkClient: NetworkClient,
    private val appFileResolver: AppFileResolver,
    appContext: Context,
) : LightNovelSourceInterface {

    private val coverLookup = ElscioneCoverLookup(networkClient, appContext)

    override val id: String = "elscione"

    @StringRes
    override val nameStrId: Int = R.string.source_name_elscione

    override val baseUrl: String = "https://server.elscione.com"
    override val catalogUrl: String = "$baseUrl$CATALOG_ROOT_PATH"
    override val language: LanguageCode = LanguageCode.ENGLISH

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val rootCacheLock = Mutex()
    @Volatile private var rootCache: List<BookResult>? = null
    @Volatile private var rootCacheAtMs: Long = 0L

    /**
     * EPUBs on this server are 10–25 MB and the shared NetworkClient enforces a 30 s read
     * timeout that reliably trips during a single download on cellular / spotty Wi-Fi.
     * Use a dedicated client with a much longer read timeout and no extra interceptors —
     * we don't need Cloudflare verification or response decoding for a binary EPUB.
     */
    private val epubDownloader: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Serializable
    private data class H5aiResponse(val items: List<H5aiItem> = emptyList())

    @Serializable
    private data class H5aiItem(
        val href: String = "",
        val time: Long? = null,
        val size: Long? = null,
        val managed: Boolean = false,
        val fetched: Boolean = false,
    )

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("index=$index") {
                paginate(loadRoot(), index)
            }
        }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String,
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index, input=$input") {
            val needle = input.trim()
            if (needle.isEmpty()) return@tryConnect PagedList.createEmpty(index)
            val matches = loadRoot().filter { it.title.contains(needle, ignoreCase = true) }
            paginate(matches, index)
        }
    }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            tryConnect("bookUrl=$bookUrl") {
                val bookPath = bookUrl.removePrefix(baseUrl).ensureTrailingSlash()
                val seriesName = seriesNameFromBookPath(bookPath)
                val (epubFiles, pdfCount) = collectFiles(bookPath)

                if (epubFiles.isEmpty()) {
                    Log.i(TAG, "no EPUBs at $bookPath (pdfCount=$pdfCount)")
                    if (pdfCount > 0) {
                        listOf(
                            ChapterResult(
                                title = "(PDF-only series — not supported in NovelReader)",
                                url = "$bookUrl#pdf-only",
                            ),
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    // Sort on the raw filename (case-insensitive, natural-numeric) before
                    // building the cleaned-up titles. The cleaned title can drop different
                    // amounts of prefix per file when the series name varies slightly,
                    // which scrambles the order; the original filename is the consistent
                    // ordering signal.
                    epubFiles
                        .sortedWith(compareBy(NATURAL_ORDER) { it.lowercase() })
                        .map { href ->
                            ChapterResult(
                                title = chapterTitleFromHref(href, seriesName),
                                url = baseUrl + href,
                            )
                        }
                }
            }
        }

    /**
     * EPUBs on this server are 10–25 MB. The chapter download pipeline fetches the chapter
     * URL once before our [getChapterText] runs, parses the response as HTML and discards
     * the bytes. Pointing a 22 MB EPUB through that path triggers timeouts and doubles the
     * bandwidth.
     *
     * We redirect that initial fetch to the h5ai info page (~2 KB), encoding the real EPUB
     * URL in a query parameter. [getChapterText] then recovers the original URL from
     * `doc.location()` and downloads the EPUB exactly once.
     */
    override suspend fun transformChapterUrl(url: String): String {
        if (url.endsWith("#pdf-only") || url.contains(REDIRECT_PARAM)) return url
        val encoded = URLEncoder.encode(url, "UTF-8")
        return "$baseUrl$REDIRECT_PATH?$REDIRECT_PARAM=$encoded"
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.IO) {
            tryConnect("bookUrl=$bookUrl") {
                val folder = appFileResolver.getLocalBookFolderName(bookUrl)
                val coverFile = appFileResolver.getStorageBookCoverImageFile(folder)
                if (coverFile.exists() && coverFile.length() > 0L) {
                    // Append the file's mtime as a cache-bust fragment so Coil/Compose see
                    // a new value after each volume's cover overwrite. AppFileResolver
                    // strips the fragment when resolving the on-disk path.
                    "${appFileResolver.getLocalBookCoverPath()}#${coverFile.lastModified()}"
                } else {
                    val title = bookTitleFromBookUrl(bookUrl) ?: return@tryConnect null
                    coverLookup.lookup(bookUrl, title)
                }
            }
        }

    private fun bookTitleFromBookUrl(bookUrl: String): String? {
        if (!bookUrl.startsWith(baseUrl)) return null
        val bookPath = bookUrl.removePrefix(baseUrl).ensureTrailingSlash()
        val title = folderTitleFromHref(bookPath)
        return title.takeIf { it.isNotBlank() }
    }

    override suspend fun getChapterTitle(doc: Document): String? {
        val real = recoverChapterUrl(doc.location()) ?: return null
        val path = real.substringAfter(baseUrl, "").ifEmpty { return null }
        return chapterTitleFromHref(path, seriesNameFromHref(path))
    }

    override suspend fun getChapterText(doc: Document): String? = withContext(Dispatchers.IO) {
        val location = doc.location()
        if (location.endsWith("#pdf-only")) return@withContext PDF_ONLY_BODY

        val url = recoverChapterUrl(location) ?: run {
            Log.w(TAG, "getChapterText: could not recover original url from $location")
            return@withContext null
        }
        val urlPath = url.removePrefix(baseUrl).substringBefore('?').substringBefore('#')
        if (!urlPath.lowercase().endsWith(".epub")) {
            Log.w(TAG, "getChapterText: not an .epub url: $url")
            return@withContext null
        }

        // Publish a small initial fraction so the UI shows the bar before any bytes arrive.
        ChapterDownloadProgress.update(url, 0.01f)
        try {
            val started = System.currentTimeMillis()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/epub+zip, application/octet-stream, */*")
                .build()
            epubDownloader.newCall(request).execute().use { r ->
                if (!r.isSuccessful) {
                    Log.w(TAG, "getChapterText: HTTP ${r.code} for $url")
                    return@withContext null
                }
                val totalBytes = r.body.contentLength()
                val epub = ProgressInputStream(
                    delegate = r.body.byteStream(),
                    total = totalBytes,
                    onProgress = { fraction -> ChapterDownloadProgress.update(url, fraction) },
                ).use { epubParser(it) }
                if (epub.chapters.isEmpty()) {
                    Log.w(TAG, "getChapterText: epub had no chapters: $url")
                    return@withContext null
                }
                val bookUrl = url.substringBeforeLast('/') + "/"
                val volumeKey = volumeKeyFromChapterUrl(url)
                val savedPaths = extractEpubImages(bookUrl, volumeKey, epub)
                saveCoverIfPresent(bookUrl, epub)
                val body = renderEpubAsBody(epub, volumeKey, savedPaths)
                val elapsed = System.currentTimeMillis() - started
                Log.i(TAG, "getChapterText: ${epub.chapters.size} chapters, saved ${savedPaths.size}/${epub.images.size} images, ${body.length} chars, ${elapsed}ms from $url")
                body
            }
        } catch (e: IOException) {
            Log.e(TAG, "getChapterText network failed for $url: ${e.javaClass.simpleName}: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "getChapterText parse failed for $url", e)
            null
        } finally {
            // Bar disappears once `chapterWithContext.downloaded` flips true on the chapter
            // row; this just stops emitting in-flight progress for the URL.
            ChapterDownloadProgress.clear(url)
        }
    }

    /**
     * Wraps a binary download stream and reports the downloaded fraction to
     * [ChapterDownloadProgress] as bytes arrive. We throttle to one update per ~64 KB read
     * so we don't thrash the StateFlow on every tiny socket read.
     */
    private class ProgressInputStream(
        private val delegate: InputStream,
        private val total: Long,
        private val onProgress: (Float) -> Unit,
    ) : FilterInputStream(delegate) {
        private var read: Long = 0
        private var lastReportedAt: Long = 0

        override fun read(): Int {
            val b = delegate.read()
            if (b != -1) {
                read += 1
                report()
            }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = delegate.read(b, off, len)
            if (n > 0) {
                read += n
                report()
            }
            return n
        }

        private fun report() {
            if (total <= 0) return
            if (read - lastReportedAt < REPORT_EVERY_BYTES && read < total) return
            lastReportedAt = read
            onProgress((read.toFloat() / total.toFloat()).coerceIn(0f, 1f))
        }

        companion object {
            private const val REPORT_EVERY_BYTES = 64L * 1024
        }
    }

    /**
     * Write the EPUB's images under a per-volume namespace so volumes of the same series
     * don't overwrite each other (every EPUB has its own `OEBPS/Images/cover.jpg` etc.,
     * and the file path on disk previously collapsed all of them onto the first volume's
     * file). Each volume's images live at `<bookFolder>/<volumeKey>/<absPath>`; the body
     * renderer rewrites each `<img src>` to include the same `<volumeKey>/` prefix so the
     * reader's resolver finds the correct file.
     *
     * Skips SVG entries — Glide can't render SVG without an extra module — and zero-byte
     * entries. Returns the set of original absPaths that were successfully made available
     * on disk.
     */
    private fun extractEpubImages(
        bookUrl: String,
        volumeKey: String,
        epub: EpubBook,
    ): Set<String> {
        val saved = mutableSetOf<String>()
        for (image in epub.images) {
            if (image.absPath.lowercase().endsWith(".svg")) continue
            if (image.image.isEmpty()) continue
            val namespacedPath = "$volumeKey/${image.absPath}"
            val target = appFileResolver.getStorageBookImageFile(bookUrl, namespacedPath)
            try {
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    target.writeBytes(image.image)
                }
                saved += image.absPath
            } catch (e: Exception) {
                Log.w(TAG, "extractEpubImages: failed to write ${target.absolutePath}: ${e.message}")
            }
        }
        return saved
    }

    /**
     * Save the EPUB's cover image to the standard cover location for this remote book so
     * `getBookCoverImageUrl` can return it later. The cover is keyed by the base64-encoded
     * book URL, matching how `AppFileResolver` resolves `local://__cover_image` for
     * HTTPS-backed books.
     *
     * Each volume read overwrites the previous file so the latest read volume's cover
     * always wins, per the user's "each time I read a new volume, take the cover of that
     * volume and use it as the overall cover" expectation.
     */
    private fun saveCoverIfPresent(bookUrl: String, epub: EpubBook) {
        val cover = epub.coverImage ?: return
        if (cover.image.isEmpty()) return
        val folder = appFileResolver.getLocalBookFolderName(bookUrl)
        val target = appFileResolver.getStorageBookCoverImageFile(folder)
        try {
            target.parentFile?.mkdirs()
            target.writeBytes(cover.image)
        } catch (e: Exception) {
            Log.w(TAG, "saveCoverIfPresent: failed to write ${target.absolutePath}: ${e.message}")
        }
    }

    /**
     * Concatenate the EPUB's chapters into one body. For each chapter, prepend the chapter
     * title (the parser strips `<h1>`–`<h6>` from the body, exposing the title separately) so
     * users see the volume's internal chapter headings — the reader's own `ReaderItem.Title`
     * only carries the volume-level title. The title is uppercased and bracketed by
     * full-width separator paragraphs so it stands out against body text in a reader that
     * only renders plain paragraphs.
     *
     * Image markers are rewritten to include the volume-key prefix so each volume's images
     * resolve to its own folder. Markers whose target image isn't available on disk (SVG,
     * missing path) get substituted with a plain-text scene-break so the reader doesn't
     * render Glide's error placeholder as a giant exclamation icon.
     */
    private fun renderEpubAsBody(
        epub: EpubBook,
        volumeKey: String,
        renderableImages: Set<String>,
    ): String {
        return buildString {
            epub.chapters.forEachIndexed { i, chapter ->
                if (i > 0) append("\n\n")
                if (chapter.title.isNotBlank()) {
                    append(formatChapterHeading(chapter.title.trim()))
                    append("\n\n")
                }
                append(rewriteImageMarkers(chapter.body, volumeKey, renderableImages))
            }
        }
    }

    private fun formatChapterHeading(title: String): String =
        "$TITLE_DIVIDER\n\n${title.uppercase()}\n\n$TITLE_DIVIDER"

    private fun rewriteImageMarkers(
        body: String,
        volumeKey: String,
        renderableImages: Set<String>,
    ): String = IMG_MARKER_REGEX.replace(body) { match ->
        val (path, yrel) = match.destructured
        if (path in renderableImages) {
            """<img src="$volumeKey/$path" yrel="$yrel">"""
        } else {
            SCENE_BREAK
        }
    }

    /**
     * Stable per-volume key derived from the EPUB filename. Used to namespace image storage
     * so volumes of the same series don't overwrite each other on disk, and embedded in
     * each `<img src>` so the reader's resolver looks under the right volume's folder.
     */
    private fun volumeKeyFromChapterUrl(url: String): String {
        val basename = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        val noExt = basename.substringBeforeLast('.', basename)
        val decoded = decodeUrl(noExt)
        // Replace path separators just in case — '/' would break the storage path.
        return decoded.replace('/', '_').replace('\\', '_')
    }

    private fun recoverChapterUrl(location: String): String? {
        if (location.isBlank()) return null
        val marker = "$REDIRECT_PARAM="
        val raw = location.substringAfter(marker, "").ifEmpty { return location }
        val encoded = raw.substringBefore('&').substringBefore('#')
        return runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrNull()
    }

    private suspend fun loadRoot(): List<BookResult> {
        val cached = rootCache
        if (cached != null && System.currentTimeMillis() - rootCacheAtMs < ROOT_CACHE_TTL_MS) {
            return cached
        }
        return rootCacheLock.withLock {
            val again = rootCache
            if (again != null && System.currentTimeMillis() - rootCacheAtMs < ROOT_CACHE_TTL_MS) {
                return@withLock again
            }
            val response = h5aiList(CATALOG_ROOT_PATH, recursive = false)
            val items = response.items
                .asSequence()
                .filter { it.href.endsWith('/') }
                .filter { it.href.startsWith(CATALOG_ROOT_PATH) && it.href != CATALOG_ROOT_PATH }
                .filter { isImmediateChild(CATALOG_ROOT_PATH, it.href) }
                .map { item ->
                    val title = folderTitleFromHref(item.href)
                    BookResult(
                        title = title,
                        url = baseUrl + item.href,
                    )
                }
                .filter { it.title.isNotEmpty() && !it.title.startsWith('!') }
                .sortedWith(compareBy(NATURAL_ORDER) { it.title })
                .toList()
            rootCache = items
            rootCacheAtMs = System.currentTimeMillis()
            Log.i(TAG, "loaded ${items.size} books from catalog root")
            items
        }
    }

    /**
     * Collect all .epub files under [rootPath] using bounded breadth-first traversal with
     * what=1 calls. Returns (epubHrefs, pdfCount). Stops at MAX_DEPTH and MAX_FILES.
     *
     * Using what=1 instead of what=2 keeps each individual response small and lets us bail
     * out cleanly if a particular subtree is slow.
     */
    private suspend fun collectFiles(rootPath: String): Pair<List<String>, Int> {
        val epubFiles = mutableListOf<String>()
        var pdfCount = 0
        val visitedFolders = mutableSetOf(rootPath)
        val frontier = ArrayDeque<Pair<String, Int>>()
        frontier += rootPath to 0

        while (frontier.isNotEmpty() && epubFiles.size < MAX_FILES) {
            val (path, depth) = frontier.removeFirst()
            val response = try {
                h5aiList(path, recursive = false)
            } catch (e: Exception) {
                Log.w(TAG, "h5ai list failed for $path", e)
                continue
            }

            for (item in response.items) {
                if (!isImmediateChild(path, item.href)) continue
                if (item.href.endsWith('/')) {
                    if (depth + 1 < MAX_DEPTH && visitedFolders.add(item.href)) {
                        frontier += item.href to depth + 1
                    }
                } else {
                    val lower = item.href.lowercase()
                    when {
                        lower.endsWith(".epub") -> epubFiles += item.href
                        lower.endsWith(".pdf") -> pdfCount++
                    }
                }
            }
        }
        Log.i(TAG, "collectFiles($rootPath): ${epubFiles.size} epubs, $pdfCount pdfs")
        return epubFiles to pdfCount
    }

    private suspend fun h5aiList(path: String, recursive: Boolean): H5aiResponse {
        val body = FormBody.Builder()
            .add("action", "get")
            .add("items[href]", path)
            .add("items[what]", if (recursive) "2" else "1")
            .build()
        val request = postRequest(
            url = "$baseUrl$API_PATH",
            headers = Headers.Builder().add("Accept", "application/json").build(),
            body = body,
        )
        val response = networkClient.call(request, followRedirects = true)
        return json.decodeFromString(response.bodyString())
    }

    private suspend fun paginate(all: List<BookResult>, index: Int): PagedList<BookResult> {
        if (all.isEmpty()) return PagedList.createEmpty(index)
        val from = index * PAGE_SIZE
        if (from >= all.size) return PagedList(list = emptyList(), index = index, isLastPage = true)
        val to = minOf(from + PAGE_SIZE, all.size)
        val slice = enrichWithCovers(all.subList(from, to))
        return PagedList(list = slice, index = index, isLastPage = to >= all.size)
    }

    /**
     * Enrich a slice of catalog [BookResult]s with cover URLs looked up from Jikan
     * (MyAnimeList). Looked-up covers are persisted on-disk so subsequent visits don't
     * re-query. Books that already have a cover (e.g., once a volume has been read and the
     * local cover file exists, [getBookCoverImageUrl] writes its `local://__cover_image`
     * value into the DB) are left untouched.
     */
    private suspend fun enrichWithCovers(books: List<BookResult>): List<BookResult> = coroutineScope {
        books.map { book ->
            async {
                if (book.coverImageUrl.isNotBlank()) return@async book
                val cover = coverLookup.lookup(book.url, book.title)
                if (cover.isNullOrBlank()) book else book.copy(coverImageUrl = cover)
            }
        }.awaitAll()
    }

    private fun isImmediateChild(parentPath: String, childHref: String): Boolean {
        if (!childHref.startsWith(parentPath)) return false
        val tail = childHref.removePrefix(parentPath).removeSuffix("/")
        return tail.isNotEmpty() && '/' !in tail
    }

    private fun folderTitleFromHref(href: String): String {
        val name = href.trimEnd('/').substringAfterLast('/')
        return decodeUrl(name)
    }

    /**
     * Drop publisher tags (`[J-Novel Club][Premium]`, `[Yen Press][Kobo]`, ...) and the
     * series-name prefix so the chapter list reads as `Volume 01`, `Volume 02 - Side Story`
     * rather than `<full series name> - Volume 01 [Publisher][Format]`.
     */
    private fun chapterTitleFromHref(href: String, seriesName: String?): String {
        val raw = decodeUrl(href.trimEnd('/').substringAfterLast('/').substringBeforeLast('.'))
        val noBrackets = TRAILING_BRACKETS_REGEX.replace(raw, "").trim()
        val stripped = if (!seriesName.isNullOrBlank() && noBrackets.startsWith(seriesName, ignoreCase = true)) {
            noBrackets.removePrefix(seriesName).trimStart(' ', '-', '_', ':', '–', '—').trim()
        } else {
            noBrackets
        }
        return stripped.ifEmpty { raw }
    }

    private fun seriesNameFromBookPath(bookPath: String): String? {
        val rel = bookPath.removePrefix(CATALOG_ROOT_PATH).trim('/')
        val first = rel.substringBefore('/', rel)
        return if (first.isEmpty()) null else decodeUrl(first)
    }

    private fun seriesNameFromHref(href: String): String? {
        val rel = href.removePrefix(CATALOG_ROOT_PATH).trim('/')
        val first = rel.substringBefore('/', "")
        return if (first.isEmpty()) null else decodeUrl(first)
    }

    private fun decodeUrl(s: String): String =
        runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

    /**
     * Looks up cover art for Elscione books by series title.
     *
     * Books in the Elscione catalog have no inline cover URLs — h5ai's directory listing
     * only exposes filenames. Until the user reads a volume locally (which lets us extract
     * the EPUB cover image), we look up covers via two providers in sequence:
     *   1. **AniList GraphQL** — indexes English / romaji / Japanese titles in one
     *      searchable field, so it tolerates the romaji titles many series use on
     *      Elscione (e.g., "Mahouka Koukou no Rettousei").
     *   2. **Jikan (MyAnimeList)** — fallback when AniList misses; uses
     *      `manga?type=lightnovel` for a tighter match on series that AniList lacks.
     *
     * For each provider we also try a few title variations (with and without trailing
     * publisher/format brackets, with and without subtitle after `:`/`-`) so a folder name
     * like "Sword Art Online - Progressive [J-Novel Club]" still resolves.
     *
     * Results are cached **permanently** on disk (`<filesDir>/elscione_cover_cache.json`)
     * — both positive hits and definitive misses — so the catalog only pays the lookup
     * cost once per book, ever. Only transient network errors skip the cache (so a future
     * online session can retry).
     */
    private class ElscioneCoverLookup(
        private val networkClient: NetworkClient,
        appContext: Context,
    ) {
        // v2: bumped from "elscione_cover_cache.json" when AniList fallback + title
        // variations were added. Old caches contain Jikan-only negatives that the new
        // permanent-cache rule would honor forever; starting fresh re-runs lookups once
        // with the broader strategy.
        private val cacheFile: File = File(appContext.filesDir, "elscione_cover_cache_v2.json")
        private val cacheMutex = Mutex()
        private val semaphore = Semaphore(MAX_CONCURRENT)
        private var cacheLoaded = false
        private val cache: MutableMap<String, CacheEntry> = mutableMapOf()

        @Serializable
        private data class CacheEntry(
            val coverUrl: String? = null,
            @SerialName("at") val lookedUpAtMs: Long = 0L,
        )

        @Serializable
        private data class CacheFile(val entries: Map<String, CacheEntry> = emptyMap())

        @Serializable
        private data class JikanResponse(val data: List<JikanItem> = emptyList())

        @Serializable
        private data class JikanItem(val images: JikanImages? = null)

        @Serializable
        private data class JikanImages(val jpg: JikanImageSet? = null)

        @Serializable
        private data class JikanImageSet(
            @SerialName("large_image_url") val largeImageUrl: String? = null,
            @SerialName("image_url") val imageUrl: String? = null,
        )

        @Serializable
        private data class AniListRequest(
            val query: String,
            val variables: Map<String, String>,
        )

        @Serializable
        private data class AniListResponse(val data: AniListData? = null)

        @Serializable
        private data class AniListData(
            @SerialName("Media") val media: AniListMedia? = null,
        )

        @Serializable
        private data class AniListMedia(val coverImage: AniListCoverImage? = null)

        @Serializable
        private data class AniListCoverImage(
            val extraLarge: String? = null,
            val large: String? = null,
            val medium: String? = null,
        )

        suspend fun lookup(bookUrl: String, bookTitle: String): String? {
            if (bookTitle.isBlank()) return null
            ensureLoaded()
            cacheMutex.withLock { cache[bookUrl] }?.let { entry ->
                // Cache is permanent for both positive hits AND definitive negatives:
                // re-querying the same title against the same APIs forever is wasteful,
                // and the user accepts that some titles have no cover.
                return entry.coverUrl
            }
            return semaphore.withPermit {
                val result = runCatching { fetchCover(bookTitle) }
                if (result.isFailure) {
                    // Transient error (network, 429, 5xx). Don't cache so a retry happens
                    // next session.
                    Log.w(LOOKUP_TAG, "cover lookup errored for $bookTitle: ${result.exceptionOrNull()?.message}")
                    return@withPermit null
                }
                val cover = result.getOrNull()
                cacheMutex.withLock {
                    cache[bookUrl] = CacheEntry(cover, System.currentTimeMillis())
                    persistLocked()
                }
                cover
            }
        }

        /**
         * Try multiple title variations against AniList first (broad cross-language
         * coverage) and Jikan as fallback. Returns the first non-blank cover URL or null
         * if every combination definitively misses. Throws on transient network failure
         * so the caller can avoid caching it.
         */
        private suspend fun fetchCover(rawTitle: String): String? {
            val candidates = titleVariations(rawTitle)
            for (candidate in candidates) {
                fetchFromAniList(candidate)?.takeIf { it.isNotBlank() }?.let { return it }
                fetchFromJikan(candidate)?.takeIf { it.isNotBlank() }?.let { return it }
            }
            return null
        }

        /**
         * Order matters — original title first, then progressively simpler variants.
         * Stops at the first variant that produces a hit, so a clean title doesn't waste
         * extra requests.
         */
        private fun titleVariations(raw: String): List<String> {
            val cleaned = raw.trim()
            val variations = linkedSetOf<String>()
            variations += cleaned
            // "Title (LN)" / "Title [Premium]" — drop trailing parens / brackets
            cleaned.replace(TRAILING_PARENS_REGEX, "").trim()
                .takeIf { it.isNotBlank() }?.let { variations += it }
            cleaned.replace(TRAILING_SQUARE_REGEX, "").trim()
                .takeIf { it.isNotBlank() }?.let { variations += it }
            // Strip both
            cleaned.replace(TRAILING_PARENS_REGEX, "")
                .replace(TRAILING_SQUARE_REGEX, "").trim()
                .takeIf { it.isNotBlank() }?.let { variations += it }
            // "Series: Subtitle" — try base series only
            cleaned.substringBefore(':').trim()
                .takeIf { it.isNotBlank() && it != cleaned }?.let { variations += it }
            // "Series - Volume X" / "Series - Side Story" — try base series only
            cleaned.substringBefore(" - ").trim()
                .takeIf { it.isNotBlank() && it != cleaned }?.let { variations += it }
            return variations.toList()
        }

        private suspend fun fetchFromJikan(title: String): String? {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val url = "$JIKAN_BASE/manga?q=$encoded&type=lightnovel&limit=1"
            val builder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
            val response = networkClient.call(builder, followRedirects = true)
            val body = response.bodyString()
            val parsed = JSON.decodeFromString<JikanResponse>(body)
            val img = parsed.data.firstOrNull()?.images?.jpg
            return img?.largeImageUrl?.takeIf { it.isNotBlank() }
                ?: img?.imageUrl?.takeIf { it.isNotBlank() }
        }

        private suspend fun fetchFromAniList(title: String): String? {
            val payload = JSON.encodeToString(
                AniListRequest(
                    query = ANILIST_QUERY,
                    variables = mapOf("search" to title),
                )
            )
            val builder = Request.Builder()
                .url(ANILIST_URL)
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            val response = networkClient.call(builder, followRedirects = true)
            val body = response.bodyString()
            val parsed = JSON.decodeFromString<AniListResponse>(body)
            val img = parsed.data?.media?.coverImage ?: return null
            return img.extraLarge?.takeIf { it.isNotBlank() }
                ?: img.large?.takeIf { it.isNotBlank() }
                ?: img.medium?.takeIf { it.isNotBlank() }
        }

        private suspend fun ensureLoaded() {
            cacheMutex.withLock {
                if (cacheLoaded) return
                cacheLoaded = true
                if (!cacheFile.exists()) return
                runCatching {
                    val raw = cacheFile.readText()
                    if (raw.isBlank()) return@runCatching
                    val parsed = JSON.decodeFromString<CacheFile>(raw)
                    cache.putAll(parsed.entries)
                }.onFailure {
                    Log.w(LOOKUP_TAG, "cover cache load failed: ${it.message}")
                }
            }
        }

        private fun persistLocked() {
            runCatching {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(JSON.encodeToString(CacheFile(cache.toMap())))
            }.onFailure {
                Log.w(LOOKUP_TAG, "cover cache write failed: ${it.message}")
            }
        }

        private companion object {
            const val LOOKUP_TAG = "ElscioneCoverLookup"
            const val JIKAN_BASE = "https://api.jikan.moe/v4"
            const val ANILIST_URL = "https://graphql.anilist.co"
            const val MAX_CONCURRENT = 3

            val JSON_MEDIA_TYPE = "application/json".toMediaType()

            // GraphQL query — `Media` returns a single best-match search result.
            // AniList searches `title.english`, `title.romaji`, `title.native`, and
            // synonyms in one shot, which gets us through romaji-named LN folders that
            // Jikan's English-leaning index misses.
            const val ANILIST_QUERY =
                "query (\$search: String) { " +
                    "Media(search: \$search, type: MANGA, format: NOVEL) { " +
                        "coverImage { extraLarge large medium } " +
                    "} " +
                "}"

            // Trailing parens / brackets like "Title (LN)" or "Title [Premium]". We use
            // both individually and combined so we cover folder names that have one or
            // both forms appended.
            val TRAILING_PARENS_REGEX = Regex("""\s*\([^)]*\)\s*$""")
            val TRAILING_SQUARE_REGEX = Regex("""\s*\[[^\]]*\]\s*$""")

            val JSON = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        }
    }

    private companion object {
        const val TAG = "Elscione"
        const val CATALOG_ROOT_PATH = "/Officially%20Translated%20Light%20Novels/"
        const val API_PATH = "/_h5ai/public/index.php"
        // Tiny endpoint (~2 KB HTML) used as a stand-in for the chapter URL that the
        // download pipeline fetches before getChapterText runs. See transformChapterUrl.
        const val REDIRECT_PATH = "/_h5ai/public/index.php"
        const val REDIRECT_PARAM = "elscione_url"
        const val PAGE_SIZE = 50
        const val ROOT_CACHE_TTL_MS = 5 * 60 * 1000L
        const val MAX_DEPTH = 4
        const val MAX_FILES = 500

        const val PDF_ONLY_BODY =
            "This series only contains PDF files. " +
                "NovelReader can read EPUB files but does not support PDFs."

        // Plain-text fallback for image markers we can't render (SVG, missing file).
        const val SCENE_BREAK = "* * *"

        // Decorative line bracketing each internal-chapter heading. The reader renders
        // every paragraph as plain text, so we lean on uppercase + heavy box-drawing rules
        // above and below the title to make headings visible.
        const val TITLE_DIVIDER = "━━━━━━━━━━━━━━━━━━━━━━━"

        // Matches BookTextMapper.ImgEntry V1 form: <img src="..." yrel="...">
        val IMG_MARKER_REGEX = Regex("""<img\s+src="([^"]*)"\s+yrel="([^"]*)"\s*>""")

        // Trailing bracket tags like " [J-Novel Club][Premium]" or "[Yen Press]"
        val TRAILING_BRACKETS_REGEX = Regex("""(\s*\[[^\[\]]*\])+\s*$""")

        // Natural-order comparator: compares numeric runs as numbers so "Volume 2" < "Volume 10".
        val NATURAL_ORDER: Comparator<String> = Comparator { a, b ->
            var i = 0
            var j = 0
            while (i < a.length && j < b.length) {
                val ca = a[i]
                val cb = b[j]
                if (ca.isDigit() && cb.isDigit()) {
                    var iEnd = i
                    while (iEnd < a.length && a[iEnd].isDigit()) iEnd++
                    var jEnd = j
                    while (jEnd < b.length && b[jEnd].isDigit()) jEnd++
                    val numA = a.substring(i, iEnd).trimStart('0').ifEmpty { "0" }
                    val numB = b.substring(j, jEnd).trimStart('0').ifEmpty { "0" }
                    val cmp = if (numA.length != numB.length) {
                        numA.length - numB.length
                    } else {
                        numA.compareTo(numB)
                    }
                    if (cmp != 0) return@Comparator cmp
                    i = iEnd
                    j = jEnd
                } else {
                    val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                    if (cmp != 0) return@Comparator cmp
                    i++
                    j++
                }
            }
            a.length - b.length
        }
    }
}
